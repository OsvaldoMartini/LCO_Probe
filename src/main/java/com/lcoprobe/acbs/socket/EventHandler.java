package com.lcoprobe.acbs.socket;

import com.lcoprobe.acbs.domain.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.core.annotation.HandleAfterCreate;
import org.springframework.data.rest.core.annotation.HandleAfterDelete;
import org.springframework.data.rest.core.annotation.HandleAfterSave;
import org.springframework.data.rest.core.annotation.RepositoryEventHandler;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import static com.lcoprobe.acbs.socket.WebSocketConfiguration.MESSAGE_PREFIX;

/**
 * @author Osvaldo Martini
 */
// tag::code[]
@Component
@RepositoryEventHandler(Server.class) // <1>
public class EventHandler {

	private final SimpMessagingTemplate websocket; // <2>

	private final EntityLinks entityLinks;

	@Autowired
	public EventHandler(SimpMessagingTemplate websocket, EntityLinks entityLinks) {
		this.websocket = websocket;
		this.entityLinks = entityLinks;
	}

	@HandleAfterCreate // <3>
	public void newServer(Server server) {
		this.websocket.convertAndSend(
				MESSAGE_PREFIX + "/newServer", getPath(server));
	}

	@HandleAfterDelete // <3>
	public void deleteServer(Server server) {
		this.websocket.convertAndSend(
				MESSAGE_PREFIX + "/deleteServer", getPath(server));
	}

	@HandleAfterSave // <3>
	public void updateServer(Server server) {
		this.websocket.convertAndSend(
				MESSAGE_PREFIX + "/updateServer", getPath(server));
	}

	/**
	 * Take an {@link Server} and get the URI using Spring Data REST's {@link EntityLinks}.
	 *
	 * @param server
	 */
	private String getPath(Server server) {
		return this.entityLinks.linkForItemResource(server.getClass(),
				server.getId()).toUri().getPath();
	}

}
// end::code[]
