$version: "2"

namespace frontsmith

@protocolDefinition(traits: [

    smithy.api#required
    smithy.api#http
    smithy.api#httpLabel
    // smithy.api#httpQuery
    // smithy.api#httpQueryParams
])
@trait(selector: "service")
structure frontRoute {}

@frontRoute
service MyRoutes {
    operations: [Home, Profile]
}

@http(uri: "/home", method: "GET")
@readonly
operation Home {

}

@http(uri: "/profile/{id}", method: "GET")
@readonly
operation Profile {
    input := {
        @httpLabel
        @required
        id: String
        // @httpQuery("name")
        // name: String
    }
}
