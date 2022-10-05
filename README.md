# Discord (JDA) HTML Transcript Generator

[![](https://jitpack.io/v/GamingLPyt/JDA-HTML-Transcript.svg)](https://jitpack.io/#GamingLPyt/JDA-HTML-Transcript)

This is a simple program that generates a HTML transcript of a Discord channel. It uses
the [JDA](https://github.com/DV8FromTheWorld/JDA) library to connect to Discord.

## Usage

1. Download the latest release from the [releases page](https://github.com/GamingLPyt/Discord-HTML-Transcript/releases).
2. Add it to your dependencies.
3. Create a new instance of the `Transcript` class.
4. Call the `createTranscript` method with the channel, the output file as parameters.

or use jitpack

````xml

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
````

````xml

<dependencies>
    <dependency>
        <groupId>com.github.GamingLPyt</groupId>
        <artifact>JDA-HTML-Transcript</artifact>
        <version>TAG</version>
    </dependency>
</dependencies>
````

## Output

The output is a HTML file that contains the messages of the channel.
We support images, attachments, embeds, links, and reactions. Messages are processed
as markdown like **bold**, *italic*, ~~strikethrough~~, `code` and more.

![Output Example](https://camo.githubusercontent.com/731952f7424ed9e745fb311c73781c9fb2d214985c5689f686e482de2ffd5beb/68747470733a2f2f696d672e6465726f636b2e6465762f3566357130612e706e67)

## Example

Normal Usage:

```java
Transcript transcript = new Transcript();
MessageChannel channel = jda.getTextChannelById("123456789"); // The channel you want to create a transcript of
TextChannel outputChannel = jda.getTextChannelById("987654321"); // The channel where the transcript will be sent

transcript.createTranscript(channel,outputChannel);
```


More options usage:
```java
Transcript transcript = new Transcript();
MessageChannel channel = jda.getTextChannelById("123456789"); // The channel you want to create a transcript of
TextChannel outputChannel = jda.getTextChannelById("987654321"); // The channel where the transcript will be sent
String fileName = "transcript.html"; // The name of the file

transcript.createTranscript(channel,outputChannel,fileName);
```

Or with InputStream return:

````java
Transcript transcript = new Transcript();
MessageChannel channel = jda.getTextChannelById("123456789"); // The channel you want to create a transcript of

InputStream transcriptStream = transcript.createTranscript(channel);

// Do something with the stream
````