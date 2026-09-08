export const environment = {
  production: true,
  // Same-origin, relative path — this container's own nginx reverse-proxies /api/v1/* to the
  // gateway service internally (see frontend/nginx.conf). No absolute host:port here on
  // purpose: works unmodified behind whatever public IP/domain this is deployed under, and
  // no CORS handling is needed in production since the browser never crosses origins.
  apiUrl: '/api/v1',
};
