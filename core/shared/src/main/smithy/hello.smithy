$version: "2"

namespace hello

use alloy#simpleRestJson

@simpleRestJson
service JiraService {
    operations: [
        GetIssue
        GetBacklog
        GetIssuesForSprint
    ]
}

@http(method: "GET", uri: "/rest/agile/1.0/board/{boardId}/backlog")
@readonly
operation GetBacklog {
    input := {
        @required
        @httpLabel
        boardId: Integer
        @httpQuery("fields")
        fields: String = "summary,description"
    }
    output := {
        issues: Issues = []
    }
}

@http(method: "GET", uri: "/rest/agile/1.0/sprint/{sprintId}/issue")
@readonly
operation GetIssuesForSprint {
    input := {
        @required
        @httpLabel
        sprintId: Integer
        @httpQuery("jql")
        jql: String
        @httpQuery("fields")
        fields: String = "summary,description"
    }
    output := {
        issues: Issues = []
    }
}

list Issues {
    member: Issue
}

structure Issue {
    @required
    key: String
    @required
    fields: IssueFields
}

structure IssueFields {
    @required
    summary: String
    description: String
    status: Status
}

structure Status {
    @required
    name: String
}

@http(method: "GET", uri: "/rest/api/2/issue/{issueId}")
@readonly
operation GetIssue {
    input := {
        @required
        @httpLabel
        issueId: String
        @httpQuery("fields")
        fields: String = "summary,description"
    }
    output := {
        @required
        @httpPayload
        issue: Issue
    }
}

@simpleRestJson
service HelloService {
    operations: [GetHello]
}

/// Fetch a greeting for a name
@http(method: "GET", uri: "/api/hello/{name}")
@readonly
operation GetHello {
    input := {
        @required
        @httpLabel
        name: String
    }
    output := {
        @required
        greeting: String
    }
}
