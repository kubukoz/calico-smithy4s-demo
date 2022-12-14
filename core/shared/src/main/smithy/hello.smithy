$version: "2"
namespace hello

use alloy#simpleRestJson

@simpleRestJson
service HelloService {
  operations: [GetHello]
}

/// Fetch a greeting for a name
@http(method: "GET", uri: "/api/hello/{name}")
@readonly
operation GetHello {
  input := {
    @required @httpLabel name: String
  },
  output := {
    @required greeting: String
  }
}
