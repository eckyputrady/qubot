package com.eckyputrady.shorturl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import javaslang.Tuple2;
import javaslang.collection.Seq;
import javaslang.collection.Stream;
import javaslang.control.Option;
import javaslang.jackson.datatype.JavaslangModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
public class HomeController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    {
        MAPPER.registerModule(new JavaslangModule());
        MAPPER.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Value("${USER_AGENT:none}")
    private String userAgent;
    
    @RequestMapping("/")
    public String index() {
        return "Hello!";
    }

    @RequestMapping("/useragent")
    public String getUserAgent() {
        return userAgent;
    }

    @RequestMapping("/github/webhook")
    public String githubWebhook(
            @RequestHeader(value = "X-GitHub-Event") String event,
            @RequestHeader(value = "X-GitHub-Delivery") String id,
            @RequestBody String payload
    ) throws JsonProcessingException {
        log.info("ID={}, Event={}, Payload={}", id, event, payload);

        if (isPullRequest(event)) {
            PullRequestPayload pl = parsePullRequestPayload(payload);
            Seq<TrelloAction> actions = findTrelloActions(pl);
            return MAPPER.writeValueAsString(actions.map(this::executeAction));
        }

        return "";

    }

    private boolean isPullRequest(String event) {
        return event.equals("pull_request");
    }

    private PullRequestPayload parsePullRequestPayload(String payload) {
        try {
            return MAPPER.readValue(payload, PullRequestPayload.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("fail", e);
        }
    }

    private Seq<TrelloAction> findTrelloActions(PullRequestPayload payload) {
        PullRequestPayload.Type actionType = payload.getActionType();
        switch (actionType) {
            case EDITED:
            case OPENED:
                return findTrelloMentions(payload.getPullRequest().getBody());
            default:
                return Stream.empty();
        }
    }

    public static Seq<TrelloAction> findTrelloMentions(String body) {
        Pattern pattern = Pattern.compile(
                "(Fixes|Fix|Closes|Close|Relates to|Relate to)\\s+(https?://trello\\.com/c/(.*?)/(.*?))(\\s+|$).*",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        return Stream
                .gen(matcher::find)
                .takeWhile(x -> x)
                .map(ignored -> new TrelloAction(
                        TrelloAction.Type.from(matcher.group(1)),
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4)
                ));
    }

    private TrelloActionResult executeAction(TrelloAction action) {
        return new TrelloActionResult(
                action,
                true,
                Option.none()
        );
    }

    @lombok.Value
    public static class TrelloAction {

        public enum Type {
            RELATE, CLOSE, UNKNOWN;

            public static Type from(String str) {
                String lowercased = str.toLowerCase();
                return lowercased.contains("fix") || lowercased.contains("close")
                        ? CLOSE
                        : lowercased.contains("relate")
                        ? RELATE
                        : UNKNOWN;
            }
        }

        private final Type type;
        private final String cardUrl;
        private final String boardId;
        private final String cardId;
    }

    @lombok.Value
    public static class TrelloActionResult {
        private final TrelloAction action;
        private final boolean isSucceed;
        private final Option<Exception> exception;
    }

    @lombok.Value
    private static class PullRequestPayload {

        private enum Type {
            EDITED, OPENED, UNKNOWN
        }

        private final String action;
        private final Data pullRequest;

        public final Type getActionType() {
            return action.equals("edited")
                    ? Type.EDITED
                    : action.equals("opened")
                    ? Type.OPENED
                    : Type.UNKNOWN;
        }

        @lombok.Value
        private static class Data {
            private final String title;
            private final String htmlUrl;
            private final GHUser user;
            private final String body;
        }
    }

    @lombok.Value
    private static class GHUser {
        private final String login;
        private final String htmlUrl;
    }
}
