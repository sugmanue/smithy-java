package software.amazon.smithy.java.example.transcribestreaming.client;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.aws.sdkv2.auth.SdkCredentialsResolver;
import software.amazon.smithy.java.core.serde.event.EventStream;
import software.amazon.smithy.java.core.serde.event.EventStreamReader;
import software.amazon.smithy.java.example.transcribestreaming.model.Alternative;
import software.amazon.smithy.java.example.transcribestreaming.model.AudioEvent;
import software.amazon.smithy.java.example.transcribestreaming.model.AudioStream;
import software.amazon.smithy.java.example.transcribestreaming.model.LanguageCode;
import software.amazon.smithy.java.example.transcribestreaming.model.MediaEncoding;
import software.amazon.smithy.java.example.transcribestreaming.model.StartStreamTranscriptionInput;
import software.amazon.smithy.java.example.transcribestreaming.model.TranscriptResultStream;
import software.amazon.smithy.java.logging.InternalLogger;
import javax.sound.sampled.AudioSystem;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


public class TranscribeStreamingTest {

    private static final InternalLogger LOGGER = InternalLogger.getLogger(TranscribeStreamingTest.class);

    @Test
    public void testTranscribeStreaming() throws Exception {
        var credentialsProvider = DefaultCredentialsProvider.builder().profileName("smithy-java-test").build();
        // The test will be skipped if we cannot find the AWS credentials under the `smithy-java-test` profile.
        assumeTrue(canLoadAwsCredentials(credentialsProvider), "AWS credentials not available");
        LOGGER.info("Running testTranscribeStreaming");

        var client = TranscribeClient.builder()
                .putConfig(RegionSetting.REGION, "us-west-2")
                .addIdentityResolver(new SdkCredentialsResolver(credentialsProvider))
                .build();

        var eventStream = EventStream.<AudioStream>newWriter();
        var req = StartStreamTranscriptionInput.builder()
                .languageCode(LanguageCode.EN_US)
                .mediaSampleRateHertz(22050)
                .mediaEncoding(MediaEncoding.PCM)
                .audioStream(eventStream)
                .build();

        Thread.ofVirtual()
                .start(() -> streamAudio(eventStream));

        LOGGER.info("Sending request transcript request");
        var res = client.startStreamTranscription(req);

        LOGGER.info("Got transcript response: {}, reading events", res);
        var transcription = new StringBuilder();
        Thread.ofVirtual()
                .start(() -> readTranscription(transcription, res.getTranscriptResultStream().asReader()))
                .join();

        LOGGER.info("Full transcription: {}", transcription);
        assertTrue(transcription.toString().toLowerCase().startsWith("hello from the smithy"),
                "full transcript: " + transcription);
    }

    private boolean canLoadAwsCredentials(DefaultCredentialsProvider credentialsProvider) {
        try {
            credentialsProvider.resolveCredentials();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static void readTranscription(StringBuilder result, EventStreamReader<TranscriptResultStream> reader) {
        reader.forEach(ev -> {
            switch (ev) {
                case TranscriptResultStream.TranscriptEventMember te -> {
                    for (var transcriptResult : te.transcriptEvent().getTranscript().getResults()) {
                        LOGGER.info("Transcription result: isPartial={}, {}", transcriptResult.isIsPartial(), transcriptResult);
                        if (!transcriptResult.isIsPartial()) {
                            var transcript = transcriptResult.getAlternatives().stream().map(Alternative::getTranscript).filter(Objects::nonNull).findFirst().orElse(null);
                            if (transcript != null) {
                                result.append(transcript);
                            }
                        }
                    }
                }
                default -> throw new IllegalStateException("Unexpected event " + ev);
            }
        });
    }

    static void streamAudio(EventStream<AudioStream> eventStream) {
        var audioUrl = TranscribeStreamingTest.class.getResource("hello-smithy-java-22050.wav");
        try (var writer = eventStream.asWriter()) {
            for (var chunk : toIterableChunks(audioUrl)) {
                LOGGER.debug("Sending audio chunk, size: {}", chunk.remaining());
                writer.write(AudioStream.builder()
                        .audioEvent(AudioEvent.builder()
                                .audioChunk(chunk)
                                .build())
                        .build());
            }
        }
    }

    static Iterable<ByteBuffer> toIterableChunks(URL audioUrl) {
        try {
            var audioFile = new File(audioUrl.toURI());
            var format = AudioSystem.getAudioFileFormat(audioUrl);
            var audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            var bytesPerFrame = format.getFormat().getFrameSize();
            LOGGER.info("Audio stream format of {}: {}, bytesPerFrame: {}", audioFile.getName(), format, bytesPerFrame);
            var framesPerChunk = 4096;
            return () -> new Iterator<>() {
                private byte[] buf;
                private int len = -1;
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    if (done) {
                        return false;
                    }
                    buf = new byte[framesPerChunk * bytesPerFrame];
                    try {
                        len = audioInputStream.read(buf);
                        if (len == -1) {
                            done = true;
                            return false;
                        }
                        return true;
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                @Override
                public ByteBuffer next() {
                    if (done) {
                        throw new NoSuchElementException();
                    }
                    return ByteBuffer.wrap(buf, 0, len);
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
