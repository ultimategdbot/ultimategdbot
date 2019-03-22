package com.github.alex1304.ultimategdbot.api;

import java.util.Map;
import java.util.function.Consumer;

import com.github.alex1304.ultimategdbot.api.guildsettings.GuildSettingsEntry;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Represents the bot itself.
 */
public interface Bot {
	/**
	 * Gets the release verion of the bot.
	 * 
	 * @return the release verion
	 */
	String getReleaseVersion();
	
	/**
	 * Gets the discord.gg link to the support server.
	 * 
	 * @return the link to the support server
	 */
	String getSupportServerInviteLink();
	
	/**
	 * Gets the authorization link to add the bot to a server.
	 * 
	 * @return the authorization link
	 */
	String getAuthLink();
	
	/**
	 * Get the bot token.
	 * 
	 * @return the token
	 */
	String getToken();

	/**
	 * Gets the default prefix.
	 * 
	 * @return the default prefix
	 */
	String getDefaultPrefix();

	/**
	 * Gets the support server of the bot.
	 * 
	 * @return the support server
	 */
	Mono<Guild> getSupportServer();

	/**
	 * Gets the moderator role of the bot.
	 * 
	 * @return the moderator role
	 */
	Mono<Role> getModeratorRole();

	/**
	 * Gets the release channel of the bot.
	 * 
	 * @return the release channel
	 */
	String getReleaseChannel();

	/**
	 * Gets the discord client.
	 * 
	 * @return the discord client
	 */
	Flux<DiscordClient> getDiscordClients();

	/**
	 * Gets the database of the bot.
	 * 
	 * @return the database
	 */
	Database getDatabase();

	/**
	 * Gets the channel where the bot sends messages for debugging purposes.
	 * 
	 * @return a Mono emitting the debug log channel
	 */
	Mono<Channel> getDebugLogChannel();

	/**
	 * Gets the channel where the bot can send attachments for its embeds.
	 * 
	 * @return a Mono emitting the attachments channel
	 */
	Mono<Channel> getAttachmentsChannel();

	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param message the message to send
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> log(String message);

	/**
	 * Sends a message into the debug log channel.
	 * 
	 * @param spec the spec of the message to send
	 * @return a Mono emitting the message sent
	 */
	Mono<Message> log(Consumer<MessageCreateSpec> spec);

	/**
	 * Prints a Throwable's stack trace in the log channel. The message may be
	 * splitted in case it doesn't fit in 2000 characters.
	 * 
	 * @param ctx the context in which the error occured
	 * @param t the throwable to print the strack trace of
	 * @return a Flux emitting all messages sent to logs (if splitted due to
	 *         character limit), or only one message otherwise.
	 */
	Flux<Message> logStackTrace(Context ctx, Throwable t);

	/**
	 * Gets the String representation of an emoji installed on one of the emoji
	 * servers. If the emoji is not found, the returned value is the given name
	 * wrapped in colons.
	 * 
	 * @param emojiName the name of the emoji to look for
	 * @return a Mono emitting the emoji code corresponding to the given name
	 */
	Mono<String> getEmoji(String emojiName);

	/**
	 * Gets the maximum time in seconds that the bot should wait for a reply when a
	 * reply menu is open.
	 * 
	 * @return the value as int (in seconds)
	 * @see Bot#openReplyMenu(Context, Message, Map, boolean, boolean)
	 */
	int getReplyMenuTimeout();

	/**
	 * Starts the bot.
	 */
	void start();

	/**
	 * Gets the guild settings entries loaded from plugins. Unlike
	 * {@link Context#getGuildSettings()}, this does not get the values for a
	 * specific guild, it gives functions to retrieve the values from any guild.
	 * 
	 * @return an unmodifiable Map containing the guild settings keys and their
	 *         associated values.
	 */
	Map<Plugin, Map<String, GuildSettingsEntry<?, ?>>> getGuildSettingsEntries();
	
	/**
	 * Gets the command kernel of this bot.
	 * 
	 * @return the command kernel
	 */
	CommandKernel getCommandKernel();
}
