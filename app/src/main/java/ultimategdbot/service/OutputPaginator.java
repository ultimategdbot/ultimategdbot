package ultimategdbot.service;

import botrino.api.config.ConfigContainer;
import botrino.api.util.MessageTemplate;
import botrino.command.CommandContext;
import botrino.command.CommandService;
import botrino.command.menu.PageNumberOutOfRangeException;
import com.github.alex1304.rdi.finder.annotation.RdiFactory;
import com.github.alex1304.rdi.finder.annotation.RdiService;
import discord4j.core.spec.EmbedCreateFields.Field;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import ultimategdbot.Strings;
import ultimategdbot.config.UltimateGDBotConfig;

import java.util.List;
import java.util.function.UnaryOperator;

@RdiService
public final class OutputPaginator {

    private final CommandService commandService;
    private final int paginationMaxEntries;

    @RdiFactory
    public OutputPaginator(CommandService commandService, ConfigContainer configContainer) {
        this.commandService = commandService;
        var config = configContainer.get(UltimateGDBotConfig.class);
        this.paginationMaxEntries = config.paginationMaxEntries();
    }

    public Mono<Void> paginate(CommandContext ctx, List<String> list, UnaryOperator<String> contentTransformer) {
        if (list.isEmpty()) {
            return ctx.channel().createMessage(contentTransformer.apply(ctx.translate(Strings.GENERAL, "no_data"))).then();
        }
        if (list.size() <= paginationMaxEntries) {
            return ctx.channel()
                    .createMessage(contentTransformer.apply(String.join("\n", list)))
                    .then();
        }
        final var maxPage = list.size() / paginationMaxEntries;
        return commandService.interactiveMenuFactory()
                .createPaginated((ctx0, page) -> {
                    PageNumberOutOfRangeException.check(page, maxPage);
                    return Mono.just(MessageTemplate.builder()
                            .setMessageContent(contentTransformer.apply(
                                    String.join("\n", list.subList(page * paginationMaxEntries,
                                            Math.min(list.size(), (page + 1) * paginationMaxEntries)))))
                            .setEmbed(EmbedCreateSpec.create().withFields(Field.of(
                                    ctx.translate(Strings.GENERAL,"page_x", (page + 1), (maxPage + 1)),
                                    ctx.translate(Strings.GENERAL,"page_instructions"), false)))
                            .build());
                })
                .open(ctx)
                .then();
    }

    public Mono<Void> paginate(CommandContext ctx, List<String> list) {
        return paginate(ctx, list, Object::toString);
    }
}
