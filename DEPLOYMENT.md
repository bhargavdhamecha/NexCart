# Deploying NexCart to a single free-tier EC2 instance

The whole stack — Postgres, Kafka, Redis, the backend, the gateway, and the frontend (built
and served by its own nginx) — runs as six Docker containers on one host via
`docker-compose.yml`. `postgres`/`kafka`/`redis` are always-on services; `backend`/`gateway`/
`frontend` are tagged `profiles: ["full"]`, so plain `docker compose up -d` still behaves
exactly like local infra-only dev — nothing here changes that workflow.

## 0. Verify locally first (before touching EC2)

```bash
docker compose --profile full up -d --build
docker compose ps                    # all 6 services should report "healthy"
curl -I http://localhost/            # 200 from nginx
curl http://localhost/api/v1/products
```

Then in a browser at `http://localhost/`: register, log in, browse products (list + detail),
add to cart, adjust quantity, walk through checkout to the payment step, and check DevTools'
Network tab for zero CORS errors — everything is same-origin now via the frontend's nginx
reverse proxy. Also check `docker compose logs kafka` shows the notification-service consumer
actually joining its group (confirms the dual-listener fix is working, not just that the
broker started). Tear down with `docker compose --profile full down` when done.

## 1. Launch the EC2 instance

- **AMI**: Ubuntu Server 24.04 LTS (free-tier eligible, x86_64)
- **Instance type**: `t3.micro` or `t2.micro` (free tier — 1 vCPU, 1 GB RAM, burstable)
- **Storage**: 8 GB gp3 is enough; bump to 16–20 GB if you have free-tier EBS headroom to
  spare (Docker images + a swapfile + logs add up)
- **Security group inbound rules**:
  - 22 (SSH) — restrict to your IP if possible
  - 80 (HTTP) — 0.0.0.0/0
  - nothing else needs to be public — Postgres/Kafka/Redis/backend/gateway ports are all
    bound to `127.0.0.1` in `docker-compose.yml` and aren't reachable externally regardless

## 2. Install Docker + the Compose plugin (Ubuntu 24.04)

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin
sudo usermod -aG docker $USER && newgrp docker
docker compose version
```

## 3. Create and persist a 2 GB swapfile

Six containers, three of them JVMs, against 1 GB of physical RAM — this is the safety net
that makes that fit (see "Known limitations" below).

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
sudo sysctl vm.swappiness=10
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
free -h   # confirm ~1.0 GB RAM + 2.0 GB swap
```

## 4. Get the repo onto the instance

```bash
git clone <your-repo-url> nexcart && cd nexcart
```

(This assumes the repo has been pushed to a remote first — it isn't a git repo as of this
writing. If you'd rather not set up a remote yet, `scp -r` the directory instead, excluding
`node_modules/`, `build/`, `.gradle/`, and `dist/`.)

## 5. Configure secrets

```bash
cp .env.example .env
nano .env   # fill in DB password, JWT secret, B2/Resend/Razorpay creds
```

## 6. Bring the stack up

```bash
docker compose --profile full up -d --build
```

First build is slow on a `t3.micro`'s burstable CPU (Gradle + npm from scratch) — expect
5–15+ minutes. If the combined build struggles for memory, build one image at a time instead:

```bash
docker compose build backend && docker compose build gateway && docker compose build frontend
docker compose --profile full up -d
```

## 7. Verify

```bash
docker compose ps                        # all 6 healthy
curl -I http://localhost/
curl http://localhost/api/v1/products
```

From your own machine, visit `http://<ec2-public-ip>/` — register, browse, add to cart, walk
through checkout, confirm no CORS errors in DevTools.

## 8. Redeploy after a code change

```bash
git pull
docker compose --profile full up -d --build
```

## Known limitations (deliberate scope for this pass)

- **No TLS** — plain HTTP only. Don't set `APP_SECURITY_REFRESH_COOKIE_SECURE=true` until
  there's TLS in front of this (a `Secure` cookie sent over plain HTTP is silently dropped by
  browsers, breaking refresh entirely).
- **Swap-reliant sizing** — six containers/three JVMs exceed 1 GB physical RAM at their
  configured `mem_limit`/`-Xmx` ceilings (~1.48 GB total); this box relies on swap for the
  overflow and on idle/light demo traffic keeping real usage under those ceilings. Not sized
  for concurrent real load — a deliberate tradeoff for staying on the free tier.
- **Single point of failure, no backups** — Postgres data lives in one Docker volume on one
  instance's disk. Losing the instance loses the data.
- **No CI/CD** — deploys are manual, over SSH (`git pull` + rebuild, step 8 above).

Next real steps, not implemented here: TLS via Caddy (simplest — automatic Let's Encrypt) or
an ALB + ACM certificate if moving to multiple instances; upsizing off free-tier sizing once
real traffic shows up; CI/CD to build+push images and trigger redeploy automatically.
