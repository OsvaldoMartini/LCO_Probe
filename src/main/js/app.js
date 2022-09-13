"use strict";

const React = require("react");
const ReactDOM = require("react-dom");
const when = require("when");
const client = require("./client");

const follow = require("./follow"); // function to hop multiple links by "rel"

const stompClient = require("./websocket-listener");

const root = "/api";

class App extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      servers: [],
      attributes: [],
      page: 1,
      pageSize: 4,
      links: {},
      curTime: null,
    };
    this.updatePageSize = this.updatePageSize.bind(this);
    this.onCreate = this.onCreate.bind(this);
    this.onUpdate = this.onUpdate.bind(this);
    this.onDelete = this.onDelete.bind(this);
    this.onNavigate = this.onNavigate.bind(this);
    this.refreshCurrentPage = this.refreshCurrentPage.bind(this);
    this.refreshAndGoToLastPage = this.refreshAndGoToLastPage.bind(this);
    this.toFilterAttributes = this.toFilterAttributes.bind(this);
  }

  toFilterAttributes = (attributes) =>{
    let filtered = {};
      if (attributes) {
        Object.keys(attributes).filter((item, index) => {
          if (item === "host") {
            filtered[0] = item;
          }
          if (item === "serverName") {
            filtered[1] = item;
          }
          if (item === "userName") {
            filtered[2] = item;
          }
          if (item === "password") {
            filtered[3] = item;
          }
          if (item === "timeout") {
            filtered[4] = item;
          }
          if (item === "environment") {
            filtered[5] = item;
          }
          if (item === "description") {
            filtered[6] = item;
          }
        });
      }
      
      return filtered && Object.entries(filtered).map(item => {
        return item[1];
    }) || [];
  } 

  loadFromServer(pageSize) {
    follow(client, root, [{ rel: "servers", params: { size: pageSize } }])
      .then((serverCollection) => {
        return client({
          method: "GET",
          path: serverCollection.entity._links.profile.href,
          headers: { Accept: "application/schema+json" },
        }).then((schema) => {
          this.schema = schema.entity;
          this.links = serverCollection.entity._links;
          return serverCollection;
        });
      })
      .then((serverCollection) => {
        this.page = serverCollection.entity.page;
        return serverCollection.entity._embedded.servers.map((server) =>
          client({
            method: "GET",
            path: server._links.self.href,
          })
        );
      })
      .then((serverPromises) => {
        return when.all(serverPromises);
      })
      .done((servers) => {
        this.setState({
          page: this.page,
          servers: servers,
          attributes: this.toFilterAttributes(this.schema.properties),
          pageSize: pageSize,
          links: this.links,
        });
      });
  }

  // tag::on-create[]
  onCreate(newServer) {
    follow(client, root, ["servers"]).done((response) => {
      client({
        method: "POST",
        path: response.entity._links.self.href,
        entity: newServer,
        headers: { "Content-Type": "application/json" },
      });
    });
  }
  // end::on-create[]

  onUpdate(server, updatedServer) {
    client({
      method: "PUT",
      path: server.entity._links.self.href,
      entity: updatedServer,
      headers: {
        "Content-Type": "application/json",
        "If-Match": server.headers.Etag,
      },
    }).done(
      (response) => {
        /* Let the websocket handler update the state */
      },
      (response) => {
        if (response.status.code === 412) {
          alert(
            "DENIED: Unable to update " +
              server.entity._links.self.href +
              ". Your copy is stale."
          );
        }
      }
    );
  }

  onDelete(server) {
    client({ method: "DELETE", path: server.entity._links.self.href });
  }

  onNavigate(navUri) {
    client({
      method: "GET",
      path: navUri,
    })
      .then((serverCollection) => {
        this.links = serverCollection.entity._links;
        this.page = serverCollection.entity.page;

        return serverCollection.entity._embedded.servers.map((server) =>
          client({
            method: "GET",
            path: server._links.self.href,
          })
        );
      })
      .then((serverPromises) => {
        return when.all(serverPromises);
      })
      .done((servers) => {
        this.setState({
          page: this.page,
          servers: servers,
          attributes: this.toFilterAttributes(this.schema.properties),
          pageSize: this.state.pageSize,
          links: this.links,
        });
      });
  }

  updatePageSize(pageSize) {
    if (pageSize !== this.state.pageSize) {
      this.loadFromServer(pageSize);
    }
  }

  // tag::websocket-handlers[]
  refreshAndGoToLastPage(message) {
    follow(client, root, [
      {
        rel: "servers",
        params: { size: this.state.pageSize },
      },
    ]).done((response) => {
      if (response.entity._links.last !== undefined) {
        this.onNavigate(response.entity._links.last.href);
      } else {
        this.onNavigate(response.entity._links.self.href);
      }
    });
  }

  refreshCurrentPage(message) {
    follow(client, root, [
      {
        rel: "servers",
        params: {
          size: this.state.pageSize,
          page: this.state.page.number,
        },
      },
    ])
      .then((serverCollection) => {
        this.links = serverCollection.entity._links;
        this.page = serverCollection.entity.page;

        return serverCollection.entity._embedded.servers.map((server) => {
          return client({
            method: "GET",
            path: server._links.self.href,
          });
        });
      })
      .then((serverPromises) => {
        return when.all(serverPromises);
      })
      .then((servers) => {
        this.setState({
          page: this.page,
          servers: servers,
          attributes: this.toFilterAttributes(this.schema.properties),
          pageSize: this.state.pageSize,
          links: this.links,
        });
      });
  }
  // end::websocket-handlers[]

  // tag::register-handlers[]
  componentDidMount() {
    setInterval(() => {
      this.loadFromServer(this.state.pageSize);
      this.setState({
        curTime: new Date().toLocaleString(),
      });
    }, 4000);

    //this.loadFromServer(this.state.pageSize);

    stompClient.register([
      { route: "/topic/newServer", callback: this.refreshAndGoToLastPage },
      { route: "/topic/updateServer", callback: this.refreshCurrentPage },
      { route: "/topic/deleteServer", callback: this.refreshCurrentPage },
    ]);
  }
  // end::register-handlers[]

  componentWillUnmount() {
    clearInterval(this.interval);
  }

  render() {
    return (
      <div>
        <h2>Current time: {this.state.curTime}</h2>
        <CreateDialog
          attributes={this.state.attributes}
          onCreate={this.onCreate}
        />
        <ServerList
          page={this.state.page}
          servers={this.state.servers}
          links={this.state.links}
          pageSize={this.state.pageSize}
          attributes={this.state.attributes}
          onNavigate={this.onNavigate}
          onUpdate={this.onUpdate}
          onDelete={this.onDelete}
          updatePageSize={this.updatePageSize}
        />
      </div>
    );
  }
}

class CreateDialog extends React.Component {
  constructor(props) {
    super(props);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleSubmit(e) {
    e.preventDefault();
    const newServer = {};
    Object.entries(this.props.attributes).forEach((attribute) => {
      let item = ReactDOM.findDOMNode(this.refs[attribute[1]]);
      if (item && item.value) {
        newServer[attribute[1]] = item.value.trim();
      }
    });
    this.props.onCreate(newServer);
    Object.entries(this.props.attributes).forEach((attribute) => {
      let item = ReactDOM.findDOMNode(this.refs[attribute[1]]);
      if (item && item.value) {
        item.value = "";
      }
    });
    window.location = "#";
  }

  render() {
    const inputs = Object.entries(this.props.attributes).map((attribute) => (
      <p key={attribute[1]}>
        <input
          type="text"
          placeholder={attribute[1]}
          ref={attribute[1]}
          className="field"
        />
      </p>
    ));

    return (
      <div>
        <a href="#createServer">Create</a>

        <div id="createServer" className="modalDialog">
          <div>
            <a href="#" title="Close" className="close">
              X
            </a>

            <h2>Create new server monitoring</h2>

            <form>
              {inputs}
              <button onClick={this.handleSubmit}>Create</button>
            </form>
          </div>
        </div>
      </div>
    );
  }
}

class UpdateDialog extends React.Component {
  constructor(props) {
    super(props);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleSubmit(e) {
    e.preventDefault();
    const updatedServer = {};
    Object.entries(this.props.attributes).forEach((attribute) => {
      let item = ReactDOM.findDOMNode(this.refs[attribute[1]]);
      if (item && item.value) {
        updatedServer[attribute[1]] = item.value.trim();
      }});
    this.props.onUpdate(this.props.server, updatedServer);
    window.location = "#";
  }

  render() {
    const inputs = Object.entries(this.props.attributes).map((attribute, index) => (
      <p key={`${attribute[1]}_${index}`}>
        <input
          type="text"
          placeholder={attribute[1]}
          defaultValue={this.props.server.entity[attribute[1]]}
          ref={attribute[1]}
          className="field"
        />
      </p>
    ));

    const dialogId =
      "updateServer-" + this.props.server.entity._links.self.href;

    return (
      <div>
        <a href={"#" + dialogId}>Update</a>

        <div id={dialogId} className="modalDialog">
          <div>
            <a href="#" title="Close" className="close">
              X
            </a>

            <h2>Update an server</h2>

            <form>
              {inputs}
              <button onClick={this.handleSubmit}>Update</button>
            </form>
          </div>
        </div>
      </div>
    );
  }
}

class ServerList extends React.Component {
  constructor(props) {
    super(props);
    this.handleNavFirst = this.handleNavFirst.bind(this);
    this.handleNavPrev = this.handleNavPrev.bind(this);
    this.handleNavNext = this.handleNavNext.bind(this);
    this.handleNavLast = this.handleNavLast.bind(this);
    this.handleInput = this.handleInput.bind(this);
  }

  handleInput(e) {
    e.preventDefault();
    const pageSize = ReactDOM.findDOMNode(this.refs.pageSize).value;
    if (/^[0-9]+$/.test(pageSize)) {
      this.props.updatePageSize(pageSize);
    } else {
      ReactDOM.findDOMNode(this.refs.pageSize).value = pageSize.substring(
        0,
        pageSize.length - 1
      );
    }
  }

  handleNavFirst(e) {
    e.preventDefault();
    this.props.onNavigate(this.props.links.first.href);
  }

  handleNavPrev(e) {
    e.preventDefault();
    this.props.onNavigate(this.props.links.prev.href);
  }

  handleNavNext(e) {
    e.preventDefault();
    this.props.onNavigate(this.props.links.next.href);
  }

  handleNavLast(e) {
    e.preventDefault();
    this.props.onNavigate(this.props.links.last.href);
  }

  render() {
    const pageInfo = this.props.page.hasOwnProperty("number") ? (
      <h3>
        Servers - Page {this.props.page.number + 1} of{" "}
        {this.props.page.totalPages}
      </h3>
    ) : null;

    const servers = this.props.servers.map((server) => (
      <Server
        key={server.entity._links.self.href}
        server={server}
        attributes={this.props.attributes}
        onUpdate={this.props.onUpdate}
        onDelete={this.props.onDelete}
        curTime={this.props.curTime}
      />
    ));

    const navLinks = [];
    if ("first" in this.props.links) {
      navLinks.push(
        <button key="first" onClick={this.handleNavFirst}>
          &lt;&lt;
        </button>
      );
    }
    if ("prev" in this.props.links) {
      navLinks.push(
        <button key="prev" onClick={this.handleNavPrev}>
          &lt;
        </button>
      );
    }
    if ("next" in this.props.links) {
      navLinks.push(
        <button key="next" onClick={this.handleNavNext}>
          &gt;
        </button>
      );
    }
    if ("last" in this.props.links) {
      navLinks.push(
        <button key="last" onClick={this.handleNavLast}>
          &gt;&gt;
        </button>
      );
    }

    return (
      <div>
        {pageInfo}
        <div className={"p-title"}> Total Items por page:
        <input
        className={"input-text"}
          ref="pageSize"
          defaultValue={this.props.pageSize}
          onInput={this.handleInput}
        />
        </div>
        <div className={"p-title"}>
          {this.props.servers.first} - ACBS LANSA listener probe status @{" "}
          {this.props.curTime}
        </div>
        <table>
          <tbody>
            <tr>
              <th>Host</th>
              <th>User</th>
              <th>Timeout</th>
              <th>Status</th>
              <th>Last time checked</th>
            </tr>
            {servers}
          </tbody>
        </table>
        <div>{navLinks}</div>
      </div>
    );
  }
}

class Server extends React.Component {
  constructor(props) {
    super(props);
    this.handleDelete = this.handleDelete.bind(this);
  }

  handleDelete() {
    this.props.onDelete(this.props.server);
  }
  render() {
    let colour = "#80ff80";
    if (this.props.server.entity.status === "RUNNING") {
      colour = "#80ff80";
    } else if (this.props.server.entity.status == "STOPPED") {
      colour = "#ffb56a";
    } else {
      colour = "#F0E68C";
    }

    return (
      <tr
        key={"T_" + this.props.index}
        style={{ background: colour }}>
          <td>{this.props.server.entity.host}</td>
          <td>{this.props.server.entity.userName}</td>
          <td>{this.props.server.entity.timeout}</td>
          <td>{this.props.server.entity.status}</td>
          <td>{this.props.server.entity.description}</td>
        <td>
          <UpdateDialog
            key={"D_" + this.props.index}
            server={this.props.server}
            attributes={this.props.attributes}
            onUpdate={this.props.onUpdate}
          />
        </td>
        <td>
          <button onClick={this.handleDelete}>Delete</button>
        </td>
      </tr>
    );
  }
}

ReactDOM.render(<App />, document.getElementById("react"));
