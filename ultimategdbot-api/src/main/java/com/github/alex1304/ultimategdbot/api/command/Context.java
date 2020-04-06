package com.github.alex1304.ultimategdbot.api.command;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.Bot;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.rest.http.client.ClientException;
import reactor.core.publisher.Mono;

public class Context {
	
	private final Command command;
	private final MessageCreateEvent event;
	private final ArgumentList args;
	private final Bot bot;
	private final String prefixUsed;
	private final FlagSet flags;
	private final User author;
	private final MessageChannel channel;

	public Context(Command command, MessageCreateEvent event, List<String> args, FlagSet flags, Bot bot, String prefixUsed, MessageChannel channel) {
		this.command = requireNonNull(command);
		this.event = requireNonNull(event);
		this.args = new ArgumentList(requireNonNull(args));
		this.bot = requireNonNull(bot);
		this.prefixUsed = requireNonNull(prefixUsed);
		this.flags = requireNonNull(flags);
		this.author = event.getMessage().getAuthor().orElseThrow();
		this.channel = requireNonNull(channel);
	}
	
	/**
	 * Gets the command that created this context.
	 * 
	 * @return the original command
	 */
	public Command command() {
		return command;
	}

	/**
	 * Gets the message create event associated to this command.
	 *
	 * @return the event
	 */
	public MessageCreateEvent event() {
		return event;
	}

	/**
	 * Gets the arguments of the command.
	 *
	 * @return the args
	 */
	public ArgumentList args() {
		return args;
	}
	
	public FlagSet flags() {
		return flags;
	}

	/**
	 * Gets the bot instance.
	 * 
	 * @return the bot
	 */
	public Bot bot() {
		return bot;
	}

	/**
	 * Sends a message in the same channel the command was sent.
	 * 
	 * @param message the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	public Mono<Message> reply(String message) {
		return reply(spec -> spec.setContent(message));
	}
	
	/**
	 * Sends a message in the same channel the command was sent. This method
	 * supports advanced message construction.
	 * 
	 * @param spec the message content of the reply
	 * @return a Mono emitting the message sent
	 */
	public Mono<Message> reply(Consumer<? super MessageCreateSpec> spec) {
		return event.getMessage().getChannel()
				.flatMap(c -> c.createMessage(spec))
				.onErrorResume(ClientException.class, e -> {
					var author = event.getMessage().getAuthor();
					if (e.getStatus().code() != 403 || author.isEmpty()) {
						return Mono.empty();
					}
					return author.get().getPrivateChannel()
							.flatMap(pc -> pc.createMessage("I was unable to send a reply to your command in <#"
									+ event.getMessage().getChannelId().asString()
									+ ">. Make sure that I have permissions to talk and send embeds there.\nError response: `"
									+ e.getErrorResponse() + "`"))
							.onErrorResume(__ -> Mono.empty())
							.then(Mono.empty());
				});
	}

	/**
	 * Gets the prefix used in the command that created this context.
	 * 
	 * @return the prefix used
	 */
	public String prefixUsed() {
		return prefixUsed;
	}
	
	/**
	 * Gets the author of the message that created this context. It is a convenient way to do
	 * <pre>
	 * event().getMessage().getAuthor().orElseThrow();
	 * </pre>
	 * 
	 * @return the author
	 */
	public User author() {
		return author;
	}
	
	/**
	 * Gets the channel of the message that created this context. The channel was
	 * cached beforehand, so it can return a MessageChannel instance directly as
	 * opposed to a Mono of it.
	 * 
	 * @return the author
	 */
	public MessageChannel channel() {
		return channel;
	}

	@Override
	public String toString() {
		return "Context{"
				+ "command=" + command
				+ ", message=" + event.getMessage()
				+ ", args=" + args
				+ ", flags=" + flags
				+ ", prefixUsed=" + prefixUsed
				+ "}";
	}
}
