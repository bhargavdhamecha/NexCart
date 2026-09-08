export const environment = {
  production: false,
  // Gateway, not the backend directly (was :8080 — bypassed the gateway's rate limiting and
  // didn't match the CORS-allowed origin, which is configured for exactly this port). Now
  // actually wired up via angular.json's fileReplacements — previously dead code, since ng
  // serve/ng build both silently read environment.ts regardless of configuration.
  apiUrl: 'http://localhost:8081/api/v1',
};
