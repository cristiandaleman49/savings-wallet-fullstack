/**
 * Development proxy for `ng serve` (exclusively for development).
 *
 * Forwards every `/api/**` request of the dev server to the Spring Boot backend
 * so the browser calls the frontend origin and avoids CORS. The API service keeps
 * using relative `/api/v1/...` URLs. In production this file is unused: `/api` is
 * resolved by the deployment's own reverse proxy.
 */
module.exports = {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
  },
};