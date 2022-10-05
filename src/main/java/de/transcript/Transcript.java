package de.transcript;

import lombok.var;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * #  || Created ||
 * #      By   >> GamingLPyt
 * #      Date >> October 05, 2022
 * #      Time >> 17:44:00
 * #
 * #  || Project ||
 * #      Name >> DISCORD-HTML-TRANSCRIPT
 */

public class Transcript {

    private final List<String>
            imageFormats = Arrays.asList("png", "jpg", "jpeg", "gif"),
            videoFormats = Arrays.asList("mp4", "webm", "mkv", "avi", "mov", "flv", "wmv", "mpg", "mpeg"),
            audioFormats = Arrays.asList("mp3", "wav", "ogg", "flac");

    public void createTranscript(final MessageChannel messageChannel, final TextChannel textChannel) throws IOException {
        this.createTranscript(messageChannel, textChannel, null);
    }

    public void createTranscript(final MessageChannel messageChannel, final TextChannel textChannel, final String filename) throws IOException {
        textChannel.sendFiles(FileUpload.fromData(this.createTranscript(messageChannel), filename != null ? filename : "transcript.html")).queue();
    }

    public InputStream createTranscript(final MessageChannel channel) throws IOException {
        return this.generateFromMessages(channel.getIterableHistory().stream().collect(Collectors.toList()));
    }

    public InputStream generateFromMessages(final Collection<Message> messages) throws IOException {
        final File htmlTemplate = this.findFile("template.html");
        if (messages.isEmpty()) throw new IllegalArgumentException("No messages to generate a transcript from");
        final Channel channel = messages.iterator().next().getChannel();
        final Document document = Jsoup.parse(htmlTemplate, "UTF-8");
        document.outputSettings().indentAmount(0).prettyPrint(true);
        document.getElementsByClass("preamble__guild-icon")
                .first().attr("src", channel.getType().equals(ChannelType.PRIVATE)
                        ? ((PrivateChannel) channel).getUser().getEffectiveAvatarUrl()
                        : ((GuildChannel) channel).getGuild().getIconUrl()
                ); // set guild icon

        document.getElementById("transcriptTitle").text(channel.getName()); // set title
        document.getElementById("guildname").text(
                channel.getType().equals(ChannelType.PRIVATE)
                        ? ((PrivateChannel) channel).getUser().getName()
                        : ((GuildChannel) channel).getGuild().getName()
        ); // set guild name
        document.getElementById("ticketname").text(channel.getName()); // set channel name

        final Element chatLog = document.getElementById("chatlog"); // chat log
        for (final Message message : messages.stream()
                .sorted(Comparator.comparing(ISnowflake::getTimeCreated))
                .collect(Collectors.toList())) {
            final Element messageGroup = document.createElement("div");
            messageGroup.addClass("chatlog__message-group");

            if (message.getReferencedMessage() != null) {
                final Element referenceSymbol = document.createElement("div");
                referenceSymbol.addClass("chatlog__reference-symbol");

                final Element reference = document.createElement("div");
                reference.addClass("chatlog__reference");

                final var referenceMessage = message.getReferencedMessage();
                final User author = referenceMessage.getAuthor();

                final String color;
                if (channel.getType().equals(ChannelType.PRIVATE))
                    color = "#FFFFFF";
                else {
                    final Member member = ((GuildChannel) channel).getGuild().getMember(author);
                    if (member.getRoles().size() > 0) color = Formatter.toHex(member.getRoles().get(0).getColor());
                    else color = "#FFFFFF";
                }

                final Element referenceContent = document.createElement("div");

                final Element referenceAuthor = document.createElement("img");
                referenceAuthor.addClass("chatlog__reference-avatar");
                referenceAuthor.attr("src", author.getEffectiveAvatarUrl());
                referenceAuthor.attr("alt", "Avatar");
                referenceAuthor.attr("loading", "lazy");

                final Element referenceAuthorName = document.createElement("span");
                referenceAuthorName.addClass("chatlog__reference-name");
                referenceAuthorName.attr("title", author.getName());
                referenceAuthorName.attr("style", "color: " + color);
                referenceAuthorName.text(author.getName());

                final Element referenceContentContent = document.createElement("div");
                referenceContentContent.addClass("chatlog__reference-content");

                final Element referenceContentContentText = document.createElement("span");
                referenceContentContentText.addClass("chatlog__reference-link");
                referenceContentContentText.attr("onclick", "scrollToMessage(event, '" + referenceMessage.getId() + "')");

                final Element referenceEm = document.createElement("em");
                referenceEm.text(referenceMessage.getContentDisplay() != null
                        ? referenceMessage.getContentDisplay().length() > 42
                        ? referenceMessage.getContentDisplay().substring(0, 42)
                        + "..."
                        : referenceMessage.getContentDisplay()
                        : "Click to see attachment");

                referenceContentContentText.appendChild(referenceEm);
                referenceContentContent.appendChild(referenceContentContentText);

                referenceContent.appendChild(referenceAuthor);
                referenceContent.appendChild(referenceAuthorName);
                referenceContent.appendChild(referenceContentContent);

                reference.appendChild(referenceContent);
                messageGroup.appendChild(referenceSymbol);
                messageGroup.appendChild(reference);
            }

            final var author = message.getAuthor();

            final Element authorElement = document.createElement("div");
            authorElement.addClass("chatlog__author-avatar-container");

            final Element authorAvatar = document.createElement("img");
            authorAvatar.addClass("chatlog__author-avatar");
            authorAvatar.attr("src", author.getEffectiveAvatarUrl());
            authorAvatar.attr("alt", "Avatar");
            authorAvatar.attr("loading", "lazy");

            authorElement.appendChild(authorAvatar);
            messageGroup.appendChild(authorElement);

            final Element content = document.createElement("div");
            content.addClass("chatlog__messages");

            final Element authorName = document.createElement("span");
            authorName.addClass("chatlog__author-name");
            authorName.attr("title", author.getAsTag());
            authorName.text(author.getName());
            authorName.attr("data-user-id", author.getId());
            content.appendChild(authorName);

            if (author.isBot()) {
                final Element botTag = document.createElement("span");
                botTag.addClass("chatlog__bot-tag").text("BOT");
                content.appendChild(botTag);
            }

            final Element timestamp = document.createElement("span");
            timestamp.addClass("chatlog__timestamp");
            timestamp
                    .text(message.getTimeCreated().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            content.appendChild(timestamp);

            final Element messageContent = document.createElement("div");
            messageContent.addClass("chatlog__message");
            messageContent.attr("data-message-id", message.getId());
            messageContent.attr("id", "message-" + message.getId());
            messageContent.attr("title", "Message sent: "
                    + message.getTimeCreated().format(DateTimeFormatter.ofPattern("HH:mm:ss")));

            if (message.getContentDisplay().length() > 0) {
                final Element messageContentContent = document.createElement("div");
                messageContentContent.addClass("chatlog__content");

                final Element messageContentContentMarkdown = document.createElement("div");
                messageContentContentMarkdown.addClass("markdown");

                final Element messageContentContentMarkdownSpan = document.createElement("span");
                messageContentContentMarkdownSpan.addClass("preserve-whitespace");
                messageContentContentMarkdownSpan
                        .html(Formatter.format(message.getContentDisplay()));

                messageContentContentMarkdown.appendChild(messageContentContentMarkdownSpan);
                messageContentContent.appendChild(messageContentContentMarkdown);
                messageContent.appendChild(messageContentContent);

                if (message.getReactions().size() > 0) {
                    final Element reactionsContent = document.createElement("div");
                    reactionsContent.addClass("chatlog__reactions");

                    for (final MessageReaction reaction : message.getReactions()) {
                        final Element reactionContent = document.createElement("div");
                        reactionContent.addClass("chatlog__reaction");
                        reactionContent.attr("data-reaction-id", reaction.getEmoji().getAsReactionCode());
                        reactionContent.attr("data-reaction-name", reaction.getEmoji().getName());
                        reactionContent.attr("data-reaction-count", String.valueOf(reaction.getCount()));
                        reactionContent.attr("data-reaction-me", String.valueOf(reaction.isSelf()));
                        reactionContent.html(reaction.getEmoji().getFormatted());

                        final Element reactionContentCount = document.createElement("div");
                        reactionContentCount.addClass("chatlog__reaction-count");
                        reactionContentCount.text(String.valueOf(reaction.getCount()));

                        reactionContent.appendChild(reactionContentCount);
                        reactionsContent.appendChild(reactionContent);
                        messageContentContent.appendChild(reactionsContent);
                    }
                }
            }

            if (!message.getAttachments().isEmpty()) for (final Message.Attachment attach : message.getAttachments()) {
                final Element attachmentsDiv = document.createElement("div");
                attachmentsDiv.addClass("chatlog__attachment");

                final var attachmentType = attach.getFileExtension();
                if (this.imageFormats.contains(attachmentType)) {
                    final Element attachmentLink = document.createElement("a");

                    final Element attachmentImage = document.createElement("img");
                    attachmentImage.addClass("chatlog__attachment-media");
                    attachmentImage.attr("src", attach.getUrl());
                    attachmentImage.attr("alt", "Image attachment");
                    attachmentImage.attr("loading", "lazy");
                    attachmentImage.attr("title",
                            "Image: " + attach.getFileName() + Formatter.formatBytes(attach.getSize()));

                    attachmentLink.appendChild(attachmentImage);
                    attachmentsDiv.appendChild(attachmentLink);
                } else if (this.videoFormats.contains(attachmentType)) {
                    final Element attachmentVideo = document.createElement("video");
                    attachmentVideo.addClass("chatlog__attachment-media");
                    attachmentVideo.attr("src", attach.getUrl());
                    attachmentVideo.attr("alt", "Video attachment");
                    attachmentVideo.attr("controls", true);
                    attachmentVideo.attr("title",
                            "Video: " + attach.getFileName() + Formatter.formatBytes(attach.getSize()));

                    attachmentsDiv.appendChild(attachmentVideo);
                } else if (this.audioFormats.contains(attachmentType)) {
                    final Element attachmentAudio = document.createElement("audio");
                    attachmentAudio.addClass("chatlog__attachment-media");
                    attachmentAudio.attr("src", attach.getUrl());
                    attachmentAudio.attr("alt", "Audio attachment");
                    attachmentAudio.attr("controls", true);
                    attachmentAudio.attr("title",
                            "Audio: " + attach.getFileName() + Formatter.formatBytes(attach.getSize()));

                    attachmentsDiv.appendChild(attachmentAudio);
                } else {
                    final Element attachmentGeneric = document.createElement("div");
                    attachmentGeneric.addClass("chatlog__attachment-generic");

                    final Element attachmentGenericIcon = document.createElement("svg");
                    attachmentGenericIcon.addClass("chatlog__attachment-generic-icon");

                    final Element attachmentGenericIconUse = document.createElement("use");
                    attachmentGenericIconUse.attr("xlink:href", "#icon-attachment");

                    attachmentGenericIcon.appendChild(attachmentGenericIconUse);
                    attachmentGeneric.appendChild(attachmentGenericIcon);

                    final Element attachmentGenericName = document.createElement("div");
                    attachmentGenericName.addClass("chatlog__attachment-generic-name");

                    final Element attachmentGenericNameLink = document.createElement("a");
                    attachmentGenericNameLink.attr("href", attach.getUrl());
                    attachmentGenericNameLink.text(attach.getFileName());

                    attachmentGenericName.appendChild(attachmentGenericNameLink);
                    attachmentGeneric.appendChild(attachmentGenericName);

                    final Element attachmentGenericSize = document.createElement("div");
                    attachmentGenericSize.addClass("chatlog__attachment-generic-size");

                    attachmentGenericSize.text(Formatter.formatBytes(attach.getSize()));
                    attachmentGeneric.appendChild(attachmentGenericSize);

                    attachmentsDiv.appendChild(attachmentGeneric);
                }

                messageContent.appendChild(attachmentsDiv);
            }

            content.appendChild(messageContent);

            if (!message.getEmbeds().isEmpty()) for (final MessageEmbed embed : message.getEmbeds()) {
                if (embed == null) continue;
                final Element embedDiv = document.createElement("div");
                embedDiv.addClass("chatlog__embed");

                if (embed.getColor() != null) {
                    final Element embedColorPill = document.createElement("div");
                    embedColorPill.addClass("chatlog__embed-color-pill");
                    embedColorPill.attr("style",
                            "background-color: #" + Formatter.toHex(embed.getColor()));

                    embedDiv.appendChild(embedColorPill);
                }

                final Element embedContentContainer = document.createElement("div");
                embedContentContainer.addClass("chatlog__embed-content-container");

                final Element embedContent = document.createElement("div");
                embedContent.addClass("chatlog__embed-content");

                final Element embedText = document.createElement("div");
                embedText.addClass("chatlog__embed-text");

                if (embed.getAuthor() != null && embed.getAuthor().getName() != null) {
                    final Element embedAuthor = document.createElement("div");
                    embedAuthor.addClass("chatlog__embed-author");

                    if (embed.getAuthor().getIconUrl() != null) {
                        final Element embedAuthorIcon = document.createElement("img");
                        embedAuthorIcon.addClass("chatlog__embed-author-icon");
                        embedAuthorIcon.attr("src", embed.getAuthor().getIconUrl());
                        embedAuthorIcon.attr("alt", "Author icon");
                        embedAuthorIcon.attr("loading", "lazy");

                        embedAuthor.appendChild(embedAuthorIcon);
                    }

                    final Element embedAuthorName = document.createElement("span");
                    embedAuthorName.addClass("chatlog__embed-author-name");

                    if (embed.getAuthor().getUrl() != null) {
                        final Element embedAuthorNameLink = document.createElement("a");
                        embedAuthorNameLink.addClass("chatlog__embed-author-name-link");
                        embedAuthorNameLink.attr("href", embed.getAuthor().getUrl());
                        embedAuthorNameLink.text(embed.getAuthor().getName());

                        embedAuthorName.appendChild(embedAuthorNameLink);
                    } else embedAuthorName.text(embed.getAuthor().getName());

                    embedAuthor.appendChild(embedAuthorName);
                    embedText.appendChild(embedAuthor);
                }

                if (embed.getTitle() != null) {
                    final Element embedTitle = document.createElement("div");
                    embedTitle.addClass("chatlog__embed-title");

                    if (embed.getUrl() != null) {
                        final Element embedTitleLink = document.createElement("a");
                        embedTitleLink.addClass("chatlog__embed-title-link");
                        embedTitleLink.attr("href", embed.getUrl());

                        final Element embedTitleMarkdown = document.createElement("div");
                        embedTitleMarkdown.addClass("markdown preserve-whitespace")
                                .html(Formatter.format(embed.getTitle()));

                        embedTitleLink.appendChild(embedTitleMarkdown);
                        embedTitle.appendChild(embedTitleLink);
                    } else {
                        final Element embedTitleMarkdown = document.createElement("div");
                        embedTitleMarkdown.addClass("markdown preserve-whitespace")
                                .html(Formatter.format(embed.getTitle()));

                        embedTitle.appendChild(embedTitleMarkdown);
                    }
                    embedText.appendChild(embedTitle);
                }

                if (embed.getDescription() != null) {
                    final Element embedDescription = document.createElement("div");
                    embedDescription.addClass("chatlog__embed-description");

                    final Element embedDescriptionMarkdown = document.createElement("div");
                    embedDescriptionMarkdown.addClass("markdown preserve-whitespace");
                    embedDescriptionMarkdown
                            .html(Formatter.format(embed.getDescription()));

                    embedDescription.appendChild(embedDescriptionMarkdown);
                    embedText.appendChild(embedDescription);
                }

                if (!embed.getFields().isEmpty()) {
                    final Element embedFields = document.createElement("div");
                    embedFields.addClass("chatlog__embed-fields");

                    for (final MessageEmbed.Field field : embed.getFields()) {
                        final Element embedField = document.createElement("div");
                        embedField.addClass(field.isInline() ? "chatlog__embed-field-inline"
                                : "chatlog__embed-field");

                        final Element embedFieldName = document.createElement("div");
                        embedFieldName.addClass("chatlog__embed-field-name");

                        final Element embedFieldNameMarkdown = document.createElement("div");
                        embedFieldNameMarkdown.addClass("markdown preserve-whitespace");
                        embedFieldNameMarkdown.html(field.getName());

                        embedFieldName.appendChild(embedFieldNameMarkdown);
                        embedField.appendChild(embedFieldName);


                        final Element embedFieldValue = document.createElement("div");
                        embedFieldValue.addClass("chatlog__embed-field-value");

                        final Element embedFieldValueMarkdown = document.createElement("div");
                        embedFieldValueMarkdown.addClass("markdown preserve-whitespace");
                        embedFieldValueMarkdown
                                .html(Formatter.format(field.getValue()));

                        embedFieldValue.appendChild(embedFieldValueMarkdown);
                        embedField.appendChild(embedFieldValue);

                        embedFields.appendChild(embedField);
                    }

                    embedText.appendChild(embedFields);
                }

                embedContent.appendChild(embedText);

                if (embed.getThumbnail() != null) {
                    final Element embedThumbnail = document.createElement("div");
                    embedThumbnail.addClass("chatlog__embed-thumbnail-container");

                    final Element embedThumbnailLink = document.createElement("a");
                    embedThumbnailLink.addClass("chatlog__embed-thumbnail-link");
                    embedThumbnailLink.attr("href", embed.getThumbnail().getUrl());

                    final Element embedThumbnailImage = document.createElement("img");
                    embedThumbnailImage.addClass("chatlog__embed-thumbnail");
                    embedThumbnailImage.attr("src", embed.getThumbnail().getUrl());
                    embedThumbnailImage.attr("alt", "Thumbnail");
                    embedThumbnailImage.attr("loading", "lazy");

                    embedThumbnailLink.appendChild(embedThumbnailImage);
                    embedThumbnail.appendChild(embedThumbnailLink);

                    embedContent.appendChild(embedThumbnail);
                }

                embedContentContainer.appendChild(embedContent);

                if (embed.getImage() != null) {
                    final Element embedImage = document.createElement("div");
                    embedImage.addClass("chatlog__embed-image-container");

                    final Element embedImageLink = document.createElement("a");
                    embedImageLink.addClass("chatlog__embed-image-link");
                    embedImageLink.attr("href", embed.getImage().getUrl());

                    final Element embedImageImage = document.createElement("img");
                    embedImageImage.addClass("chatlog__embed-image");
                    embedImageImage.attr("src", embed.getImage().getUrl());
                    embedImageImage.attr("alt", "Image");
                    embedImageImage.attr("loading", "lazy");

                    embedImageLink.appendChild(embedImageImage);
                    embedImage.appendChild(embedImageLink);

                    embedContentContainer.appendChild(embedImage);
                }

                if (embed.getFooter() != null) {
                    final Element embedFooter = document.createElement("div");
                    embedFooter.addClass("chatlog__embed-footer");

                    if (embed.getFooter().getIconUrl() != null) {
                        final Element embedFooterIcon = document.createElement("img");
                        embedFooterIcon.addClass("chatlog__embed-footer-icon");
                        embedFooterIcon.attr("src", embed.getFooter().getIconUrl());
                        embedFooterIcon.attr("alt", "Footer icon");
                        embedFooterIcon.attr("loading", "lazy");

                        embedFooter.appendChild(embedFooterIcon);
                    }

                    final Element embedFooterText = document.createElement("span");
                    embedFooterText.addClass("chatlog__embed-footer-text");
                    embedFooterText.text(embed.getTimestamp() != null
                            ? embed.getFooter().getText() + " • " + embed.getTimestamp()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            : embed.getFooter().getText());

                    embedFooter.appendChild(embedFooterText);

                    embedContentContainer.appendChild(embedFooter);
                }

                embedDiv.appendChild(embedContentContainer);
                content.appendChild(embedDiv);
            }

            messageGroup.appendChild(content);
            chatLog.appendChild(messageGroup);
        }
        return new ByteArrayInputStream(document.outerHtml().getBytes());
    }

    private File findFile(final String fileName) {
        final URL url = this.getClass().getClassLoader().getResource(fileName);
        if (url == null) throw new IllegalArgumentException("file is not found: " + fileName);
        return new File(url.getFile());
    }
}
