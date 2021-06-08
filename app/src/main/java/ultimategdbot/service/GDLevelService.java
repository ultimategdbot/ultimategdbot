package ultimategdbot.service;

import botrino.api.i18n.Translator;
import botrino.api.util.DurationUtils;
import botrino.api.util.MessageTemplate;
import botrino.command.CommandContext;
import botrino.command.CommandFailedException;
import botrino.command.CommandService;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import jdash.client.GDClient;
import jdash.client.exception.ActionFailedException;
import jdash.client.exception.GDClientException;
import jdash.client.request.GDRequests;
import jdash.common.entity.GDLevel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import ultimategdbot.Strings;
import ultimategdbot.util.EmbedType;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import static botrino.api.util.Markdown.*;
import static reactor.function.TupleUtils.function;
import static ultimategdbot.util.GDFormatter.formatCode;
import static ultimategdbot.util.GDLevels.*;
import static ultimategdbot.util.InteractionUtils.unexpectedReply;
import static ultimategdbot.util.InteractionUtils.writeOnlyIfRefresh;

@RdiService
public final class GDLevelService {

    private final EmojiService emoji;
    private final CommandService commandService;
    private final GDClient gdClient;

    @RdiFactory
    public GDLevelService(EmojiService emoji, CommandService commandService, GDClient gdClient) {
        this.emoji = emoji;
        this.commandService = commandService;
        this.gdClient = gdClient;
    }

    public EmbedCreateSpec searchResultsEmbed(CommandContext ctx, Iterable<? extends GDLevel> results, String title,
                                              int page) {
        final var embed = EmbedCreateSpec.builder();
        embed.title(title);
        var i = 1;
        for (final var level : results) {
            final var coins = coinsToEmoji(emoji.get(level.hasCoinsVerified()
                    ? "user_coin" : "user_coin_unverified"), level.coinCount(), true);
            final var difficultyEmoji = emoji.get(getDifficultyEmojiForLevel(level));
            final var song = level.song().map(s -> formatSong(ctx, s))
                    .orElse(":warning: " + ctx.translate(Strings.GD, "song_unknown"));
            embed.addField(String.format("`%02d` - %s %s | __**%s**__ by **%s** %s%s",
                    i,
                    difficultyEmoji + (level.stars() > 0 ? " " + emoji.get("star") + " x" + level.stars() : ""),
                    coins.equals("None") ? "" : ' ' + coins,
                    level.name(),
                    level.creatorName().orElse("-"),
                    level.originalLevelId().orElse(0L) > 0 ? emoji.get("copy") : "",
                    level.objectCount() > 40_000 ? emoji.get("object_overflow") : ""),
                    String.format("%s %d \t\t %s %d \t\t %s %s\n:musical_note:  **%s**\n _ _",
                            emoji.get("downloads"),
                            level.downloads(),
                            emoji.get(level.likes() >= 0 ? "like" : "dislike"),
                            level.likes(),
                            emoji.get("length"),
                            level.length(),
                            song), false);
            i++;
        }
        if (i == 1) {
            embed.description(italic(ctx.translate(Strings.GD, "no_results")));
        }
        embed.addField(ctx.translate(Strings.APP, "page_x", page + 1, "??"),
                ctx.translate(Strings.APP, "page_instructions") + '\n'
                        + ctx.translate(Strings.GD, "select_result"), false);
        return embed.build();
    }

    public Mono<EmbedCreateSpec> detailedEmbed(CommandContext ctx, long levelId, String creatorName, EmbedType type) {
        final var gdClient = writeOnlyIfRefresh(ctx, this.gdClient);
        return gdClient.downloadLevel(levelId)
                .zipWhen(level -> extractSongParts(ctx, level))
                .map(function((level, songParts) -> {
                    final var embed = EmbedCreateSpec.builder();
                    embed.author(type.getAuthorName(ctx), null, type.getAuthorIconUrl());
                    embed.thumbnail(getDifficultyImageForLevel(level));
                    final var title = emoji.get("play") + "  __" + level.name() + "__ by " +
                            level.creatorName().orElse(creatorName);
                    final var desc = bold(ctx.translate(Strings.GD, "label_description")) + ' ' +
                            (level.description().isEmpty()
                                    ? italic('(' + ctx.translate(Strings.GD, "no_description") + ')')
                                    : escape(level.description()));
                    final var coins = formatCoins(ctx, level);
                    final var downloadLikesLength = formatDownloadsLikesLength(level);
                    var objCount = bold(ctx.translate(Strings.GD, "label_object_count")) + ' ';
                    if (level.objectCount() > 0 || level.levelVersion() >= 21) {
                        if (level.objectCount() == 65535) {
                            objCount += ">";
                        }
                        objCount += level.objectCount();
                    } else {
                        objCount += italic(ctx.translate(Strings.APP, "unknown"));
                    }
                    objCount += '\n';
                    final var extraInfo = new StringBuilder();
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_level_id"))).append(' ')
                            .append(level.id()).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_level_version"))).append(' ')
                            .append(level.levelVersion()).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_game_version"))).append(' ')
                            .append(formatGameVersion(level.gameVersion())).append('\n');
                    extraInfo.append(objCount);
                    var pass = "";
                    if (level.copyPasscode().isEmpty() && level.isCopyable()) {
                        pass = ctx.translate(Strings.GD, "free_to_copy");
                    } else if (level.copyPasscode().isEmpty()) {
                        pass = ctx.translate(Strings.APP, "no");
                    } else {
                        pass = ctx.translate(Strings.GD, "protected_copyable", emoji.get("lock"),
                                String.format("%06d", level.copyPasscode().orElseThrow()));
                    }
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_copyable"))).append(' ')
                            .append(pass).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_uploaded"))).append(' ')
                            .append(ctx.translate(Strings.APP, "ago", level.uploadedAgo())).append('\n');
                    extraInfo.append(bold(ctx.translate(Strings.GD, "label_last_updated"))).append(' ')
                            .append(ctx.translate(Strings.APP, "ago", level.updatedAgo())).append('\n');
                    if (level.originalLevelId().orElse(0L) > 0) {
                        extraInfo.append(emoji.get("copy")).append(' ')
                                .append(bold(ctx.translate(Strings.GD, "label_original"))).append(' ')
                                .append(level.originalLevelId().orElseThrow()).append('\n');
                    }
                    if (level.objectCount() > 40_000) {
                        extraInfo.append(emoji.get("object_overflow")).append(' ')
                                .append(bold(ctx.translate(Strings.GD, "lag_notice"))).append('\n');
                    }
                    embed.addField(title, desc, false);
                    embed.addField(coins, downloadLikesLength + "\n_ _", false);
                    embed.addField(":musical_note:   " + songParts.getT1(),
                            songParts.getT2() + "\n_ _\n" + extraInfo, false);
                    return embed.build();
                }));
    }

    public Mono<EmbedCreateSpec> compactEmbed(Translator tr, GDLevel level, EmbedType type) {
        return extractSongParts(tr, level).map(Tuple2::getT1)
                .map(songInfo -> {
                    final var embed = EmbedCreateSpec.builder();
                    embed.author(type.getAuthorName(tr), null, type.getAuthorIconUrl());
                    embed.thumbnail(getDifficultyImageForLevel(level));
                    final var title =
                            emoji.get("play") + "__" + level.name() + "__ by " + level.creatorName().orElse("-") +
                            (level.originalLevelId().orElse(0L) > 0 ? ' ' + emoji.get("copy") : "") +
                            (level.objectCount() > 40_000 ? ' ' + emoji.get("object_overflow") : "");
                    final var coins = formatCoins(tr, level);
                    final var downloadLikesLength = formatDownloadsLikesLength(level);
                    embed.addField(title, downloadLikesLength, false);
                    embed.addField(coins, ":musical_note:   " + songInfo, false);
                    embed.footer(tr.translate(Strings.GD, "label_level_id") + ' ' + level.id(), null);
                    return embed.build();
                });
    }

    public Mono<Void> interactiveSearch(CommandContext ctx, String title,
                                        IntFunction<? extends Flux<? extends GDLevel>> searchFunction) {
		final var resultsOfCurrentPage = new AtomicReference<List<? extends GDLevel>>();
        return searchFunction.apply(0).collectList()
                .doOnNext(resultsOfCurrentPage::set)
                .flatMap(results -> results.size() == 1 ? sendSelectedSearchResult(ctx, results.get(0), false)
                        : commandService.interactiveMenuFactory()
                        .createPaginated((tr, page) -> searchFunction.apply(page).collectList()
                                .doOnNext(resultsOfCurrentPage::set)
                                .map(newResults -> MessageTemplate.builder()
                                        .setEmbed(searchResultsEmbed(ctx, newResults, title, page))
                                        .build()))
                        .addMessageItem("select", interaction -> {
                            if (interaction.getInput().getArguments().size() < 2) {
                                return unexpectedReply(ctx, ctx.translate(Strings.GD, "error_select_not_specified"));
                            }
                            final var selectedInput = interaction.getInput().getArguments().get(1);
                            if (!selectedInput.matches("[0-9]{1,2}")) {
                                return unexpectedReply(ctx, ctx.translate(Strings.GD, "error_invalid_input"));
                            }
                            final var currentResults = resultsOfCurrentPage.get();
                            final var selected = Integer.parseInt(selectedInput) - 1;
                            if (selected < 0 || selected >= currentResults.size()) {
                                return unexpectedReply(ctx, ctx.translate(Strings.GD, "error_select_not_existing"));
                            }
                            return sendSelectedSearchResult(ctx, currentResults.get(selected), true);
                        })
                        .open(ctx)
                        .then());
    }

    private Mono<Void> sendSelectedSearchResult(CommandContext ctx, GDLevel level, boolean withCloseOption) {
        return detailedEmbed(ctx, level.id(), level.creatorName().orElse("-"), EmbedType.LEVEL_SEARCH_RESULT)
                .flatMap(embed -> !withCloseOption ? ctx.channel().createEmbed(embed).then()
                        : commandService.interactiveMenuFactory().create(MessageCreateSpec.create().withEmbed(embed))
                        .addReactionItem(commandService.interactiveMenuFactory()
                                .getPaginationControls()
                                .getCloseEmoji(), interaction -> Mono.empty())
                        .deleteMenuOnClose(true)
                        .open(ctx)
                        .then());
    }

    public Mono<Message> sendTimelyInfo(CommandContext ctx, boolean isWeekly) {
        final var gdClient = writeOnlyIfRefresh(ctx, this.gdClient);
        final var timelyMono = isWeekly ? gdClient.getWeeklyDemonInfo() : gdClient.getDailyLevelInfo();
        final var downloadId = isWeekly ? -2 : -1;
        final var type = isWeekly ? EmbedType.WEEKLY_DEMON : EmbedType.DAILY_LEVEL;
        return timelyMono
                .flatMap(timely -> detailedEmbed(ctx, downloadId, "-", type)
                        .flatMap(embed -> ctx.channel()
                                .createMessage(ctx.translate(Strings.GD, "timely_of_today",
                                        type.getAuthorName(ctx), DurationUtils.format(timely.nextIn())))
                                .withEmbed(embed)))
                .onErrorMap(e -> e instanceof GDClientException
                                && ((GDClientException) e).getRequest().getUri().equals(GDRequests.GET_GJ_DAILY_LEVEL)
                                && e.getCause() instanceof ActionFailedException,
                        e -> new CommandFailedException(
                                ctx.translate(Strings.GD, "error_no_timely_set", type.getAuthorName(ctx))));
    }

    private Mono<Tuple2<String, String>> extractSongParts(Translator tr, GDLevel level) {
        return level.song().map(Mono::just)
                .or(() -> level.songId().map(gdClient::getSongInfo))
                .map(songMono -> songMono.map(s -> Tuples.of(formatSong(tr, s),
                        formatSongExtra(tr, s, emoji.get("play"), emoji.get("download_song")))))
                .orElseGet(() -> Mono.just(unknownSongParts(tr)))
                .onErrorReturn(e -> e instanceof GDClientException
                        && ((GDClientException) e).getRequest().getUri().equals(GDRequests.GET_GJ_SONG_INFO)
                        && e.getCause() instanceof ActionFailedException
                        && e.getCause().getMessage().equals("-2"), bannedSongParts(tr))
                .onErrorReturn(unknownSongParts(tr));
    }

    private String formatCoins(Translator tr, GDLevel level) {
        return tr.translate(Strings.GD, "label_coins") + ' ' +
                coinsToEmoji(emoji.get(level.hasCoinsVerified()
                        ? "user_coin" : "user_coin_unverified"), level.coinCount(), false);
    }

    private String formatDownloadsLikesLength(GDLevel level) {
        final var dlWidth = 9;
        return emoji.get("downloads") + ' ' +
                formatCode(level.downloads(), dlWidth) + '\n' +
                emoji.get(level.likes() >= 0 ? "like" : "dislike") + ' ' +
                formatCode(level.likes(), dlWidth) + '\n' + emoji.get("length") + ' ' +
                formatCode(level.length(), dlWidth);
    }

}