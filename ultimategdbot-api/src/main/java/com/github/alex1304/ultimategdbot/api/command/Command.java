package com.github.alex1304.ultimategdbot.api.command;

import java.util.Optional;
import java.util.Set;

import reactor.core.publisher.Mono;

/**
 * Represents a bot command.
 */
public interface Command {
	/**
	 * Defines the action of the command
	 * 
	 * @param ctx the context
	 * @return a Mono that completes empty when the command is successful, and emits
	 *         an error when something goes wrong.
	 */
	Mono<Void> run(Context ctx);

	/**
	 * Gets the aliases for this command.
	 * 
	 * @return the set of aliases
	 */
	Set<String> getAliases();
	
	/**
	 * Gets the name of the permission required to use the command, if applicable
	 * 
	 * @return the required permission, or empty Optional if no special permission
	 *         is required
	 */
	Optional<String> getRequiredPermission();
	
	/**
	 * Gets the documentation of the command.
	 * 
	 * @return the documentation
	 */
	CommandDocumentation getDocumentation();
}
