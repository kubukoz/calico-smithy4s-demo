# calico-smithy4s-demo

Small project serving as a demonstration of a full-stack Smithy4s application.

- Backend: http4s, smithy4s
- Frontend: calico, smithy4s
- CLI: decline, http4s, smithy4s

## Development

### Easy way - server+frontend

```bash
sbt
> serverJVM/run
```

This will build the frontend, the backend, and launch a server that hosts both at `localhost:8080`.
Note that there will be no file watching, no automatic recompilation, or anything of the sort.

### Hard way

This works best if you want to get quick feedback on the frontend changes.

1. Run `yarn dev` inside `web` (after an initial `yarn` to fetch dependencies)
2. Run `sbt ~frontJS/frontLink` to rebuild frontend on each core/frontend change
3. Run `sbt serverJVM/run` or `bloop run serverJVM` to run the backend.

## Dev server inner workings

The frontend is served by [Vite](https://vitejs.dev/) on an arbitrary port.
Vite sets up a reverse proxy for all `/api/**` routes that targets the backend host, which is currently
`localhost:8080`.

## Deployment

1. `sbt Docker/publishLocal`
2. Deploy Docker image on a platform of your choice

## Building the CLI

The command line client is cross-built for Scala (JVM), Scala.js (node.js) and Scala Native (native binary). To get the runnables:

- JVM: build with `cliJVM/stage`, run with `./cli/jvm/target/universal/stage/bin/cli`
- JS: build with `cliJS/fastOptJS`, run with `node ./cli/js/target/scala-3.2.0/cli-fastopt.js`
- Native: build with `cliNative/nativeLink`, run with `./cli/native/target/scala-3.2.0/cli-out`
