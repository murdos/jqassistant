package com.buschmais.jqassistant.plugin.json.impl.parsing;

import java.util.List;
import java.util.stream.Stream;

import com.buschmais.jqassistant.plugin.json.impl.parsing.generated.JSONLexer;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.buschmais.jqassistant.plugin.json.impl.parsing.generated.JSONLexer.STRING;
import static com.buschmais.jqassistant.plugin.json.impl.parsing.generated.JSONLexer.T__4;
import static com.buschmais.jqassistant.plugin.json.impl.parsing.generated.JSONLexer.T__5;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class JQAssistantJSONLexerTest {
    public static Stream<Arguments> data() {
        return Stream.of(arguments("[]", new String[]{"[", "]"}, new Integer[] {T__4, T__5}),
                         arguments("[\"VALUE\"]", new String[]{"[", "VALUE", "]"}, new Integer[]{T__4, STRING, T__5}));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void lexerOutput(String input, String[] expectedTokens, Integer[] exptectedTypeIds) {
        JSONLexer lexer = new JQAssistantJSONLexer(CharStreams.fromString(input), "/not/given");

        List<? extends Token> foundTokens = lexer.getAllTokens();

        assertThat("Number of expected and found tokens must be the same.",
                   foundTokens.size(), Matchers.is(expectedTokens.length));

        for (int i = 0; i < expectedTokens.length; i++) {
            assertThat("Expected token and found token text mismatch.",
                       foundTokens.get(i).getText(), equalTo(expectedTokens[i]));

            assertThat(foundTokens.get(i).getType(), equalTo(exptectedTypeIds[i]));
        }
    }
}
