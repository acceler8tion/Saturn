package com.acceler8tion.Saturn.entity.command.minesweeper;

import com.acceler8tion.Saturn.database.DataBase;
import com.acceler8tion.Saturn.Bot;
import com.acceler8tion.Saturn.entity.annotation.command.CommandDoc;
import com.acceler8tion.Saturn.entity.annotation.command.CommandProperties;
import com.acceler8tion.Saturn.entity.command.IGuildCommand;
import com.acceler8tion.Saturn.entity.game.MineSweeper;
import com.acceler8tion.Saturn.event.IGuildMessageReceivedEvent;
import com.acceler8tion.Saturn.util.EmojiFactory;
import com.acceler8tion.Saturn.util.PermissionLevel;
import com.acceler8tion.Saturn.util.Translator;
import com.acceler8tion.Saturn.util.Utils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;


@CommandProperties(level = PermissionLevel.VERIFIED)
@CommandDoc(
        alias = "ms.o",
        preview = "x y 좌표의 타일을 깜"
)
@SuppressWarnings("ConstantConditions")
public class OpenTileCurrentMineSweeper extends IGuildCommand {

    @Override
    public void run(IGuildMessageReceivedEvent event) {
        super.run(event);

        event.getMessage().delete().queueAfter(1000, TimeUnit.MILLISECONDS);

        String mergedIds = event.getConfiguration().getId() + event.getTextChannel().getId();
        String userId = event.getUser().getId();
        String[] splitted = event.getSplitted();
        boolean b = Objects.requireNonNull(
                DataBase.minesweeperChannelDB.extract(db -> {
                    return !db.isNull(event.getConfiguration().getId()) && db.getJSONArray(event.getConfiguration().getId()).toList().contains(event.getTextChannel().getId());
                }));
        if(b){
            if(Bot.minesweeperCenter().has(mergedIds)){
                final MineSweeper ms = Bot.minesweeperCenter().find(mergedIds);
                final String messageId = Bot.minesweeperCenter().find(mergedIds).getMessageId();

                if(splitted.length < 3) return;
                else {
                    int x, y;
                    try {
                        x = Integer.parseInt(splitted[1]) - 1;
                        y = Integer.parseInt(splitted[2]) - 1;
                    } catch (NumberFormatException e){
                        return;
                    }
                    if(!ms.isSafe(x, y)) return;
                    else {
                        if(!ms.getParticipants().contains(userId)) ms.addParticipants(userId);
                        MineSweeper.Display display = ms.open(x, y, false);
                        display = ms.isDie() ? MineSweeper.Display.DIED : display;
                        MineSweeper.Display finalDisplay = display;
                        Translator.doOnTranslate(event.getConfiguration().getLang(), "ms_ongoing_script", event, (s, ievent) -> {
                            event.getTextChannel().editMessageById(messageId, String.format(s, ms.getX(), ms.getY(), ms.getBomb(), EmojiFactory.ANGRY_FACE_WITH_HORNS.getEmoji(), ms.getRate(), finalDisplay.name(), ms.display(finalDisplay, x, y), ms.getLeftBomb(), ms.getParticipants().size(), Utils.timeIndicator(System.currentTimeMillis() - ms.getStartAt()))).queue();
                            if(finalDisplay == MineSweeper.Display.ONGOING) Bot.minesweeperCenter().register(mergedIds, ms);
                            else Bot.minesweeperCenter().unregister(mergedIds);
                        });
                    }
                }
            } else {
                Translator.doOnTranslate(event.getConfiguration().getLang(), "ms_open_no_game", event, (s, ievent) -> {
                    event.getTextChannel().sendMessage(String.format(s, EmojiFactory.CONFUSED_FACE.getEmoji())).queue(m -> m.addReaction(EmojiFactory.WHITE_CHECK_MARK.getEmoji()).queue());
                });
            }
        } else {
            Translator.doOnTranslate(event.getConfiguration().getLang(), "ms_start_unregistered_channel", event, (s, ievent) -> {
                ievent.getTextChannel().sendMessage(String.format(s, ievent.getGuildChannel().getId(), EmojiFactory.SAD_BUT_RELIEVED_FACE.getEmoji())).queue(m -> m.addReaction(EmojiFactory.WHITE_CHECK_MARK.getEmoji()).queue());
            });
        }
    }

}
