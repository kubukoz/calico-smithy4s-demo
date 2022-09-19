# calica-smithy4s-demo

Small project serving as a demonstration of a full-stack Smithy4s application.

- Backend: http4s, smithy4s
- Frontend: calica, smithy4s

## Development

### Easy way

```bash
sbt
> serverJVM/run
```

This will build the frontend, the backend, and launch a server that hosts both at `localhost:8080`.
Note that there will be no file watching, no automatic recompilation, or anything of the sort.

### Hard way

This works best if you want to get quick feedback on the frontend changes.

1. Run `yarn dev` inside `web` (after an initial `yarn` to fetch dependencies)
2. Run `sbt ~front/frontLink` to rebuild frontend on each core/frontend change
3. Run `sbt serverJVM/run` or `bloop run serverJVM` to run the backend.

## Dev server inner workings

The frontend is served by [Vite](https://vitejs.dev/) on an arbitrary port.
Vite sets up a reverse proxy for all `/api/**` routes that targets the backend host, which is currently
`localhost:8080`.

## Deployment

TODO. In general, for small applications you'll want to directly serve resources from http4s.

- Run a dist task on the yarn build.
- Put the output into a location that http4s can serve resources from
- Package everything into a single Docker image with the Scala application.
