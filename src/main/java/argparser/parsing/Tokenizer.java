package argparser.parsing;

import argparser.Command;
import argparser.Token;
import argparser.TokenType;
import argparser.parsing.errors.TokenizeError;
import argparser.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class Tokenizer extends ParsingStateBase<TokenizeError> {
	protected boolean tupleOpen = false;
	protected boolean stringOpen = false;
	private boolean finishedTokenizing = false;
	private int currentCharIndex = 0;
	public final Pair<Character, Character> tupleChars;
	private final List<Token> finalTokens = new ArrayList<>();
	private String input;
	private char[] inputChars;

	public Tokenizer(Command command) {
		super(command);
		this.tupleChars = command.getTupleChars().getCharPair();
	}

	void addError(TokenizeError.TokenizeErrorType type, int index) {
		this.addError(new TokenizeError(type, index));
	}

	public void setInput(String input) {
		this.input = input;
		this.inputChars = input.toCharArray();
	}

	private void addToken(TokenType type, String contents) {
		this.finalTokens.add(new Token(type, contents));
	}

	private boolean isCharAtRelativeIndex(int index, char character) {
		index += this.currentCharIndex;
		if (index >= this.inputChars.length || index < 0) return false;
		return this.inputChars[index] == character;
	}


	public void tokenize(String content) {
		this.finishedTokenizing = false; // just in case we are tokenizing again for any reason

		this.setInput(content);
		final var currentValue = new StringBuilder();

		final var values = new Object() {
			char currentStringChar = 0;
			TokenizeError.TokenizeErrorType errorType = null;
		};

		final Runnable tokenizeSection = () -> {
			final Token token = this.tokenizeSection(currentValue.toString());
			Command subCmd;
			// if this is a subcommand, continue tokenizing next elements
			if (token.type() == TokenType.SUB_COMMAND && (subCmd = getSubCommandByName(token.contents())) != null) {
				// forward the rest of stuff to the subCommand
				subCmd.getTokenizer().tokenize(content.substring(this.currentCharIndex));
				this.finishedTokenizing = true;
			} else {
				finalTokens.add(token);
			}
			currentValue.setLength(0);
		};


		for (
			this.currentCharIndex = 0;
			this.currentCharIndex < this.inputChars.length && !this.finishedTokenizing;
			this.currentCharIndex++
		) {
			char cChar = this.inputChars[this.currentCharIndex];

			// user is trying to escape a character
			if (cChar == '\\') {
				currentValue.append(this.inputChars[++this.currentCharIndex]); // skip the \ character and append the next character

				// reached a possible value wrapped in quotes
			} else if (cChar == '"' || cChar == '\'') {
				// if we are already in an open string, push the current value and close the string. Make sure
				// that the current char is the same as the one that opened the string
				if (this.stringOpen && values.currentStringChar == cChar) {
					this.addToken(TokenType.ARGUMENT_VALUE, currentValue.toString());
					currentValue.setLength(0);
					this.stringOpen = false;

					// the string is open, but the character does not match. Push it as a normal character
				} else if (this.stringOpen) {
					currentValue.append(cChar);

					// the string is not open, so open it and set the current string char to the current char
				} else {
					this.stringOpen = true;
					values.currentStringChar = cChar;
				}

				// append characters to the current value as long as we are in a string
			} else if (this.stringOpen) {
				currentValue.append(cChar);

				// reached a possible tuple start character
			} else if (cChar == this.tupleChars.first()) {
				// if we are already in a tuple, set error and stop tokenizing
				if (this.tupleOpen) {
					values.errorType = TokenizeError.TokenizeErrorType.TUPLE_ALREADY_OPEN;
					break;
				} else if (!currentValue.isEmpty()) { // if there was something before the tuple, tokenize it
					tokenizeSection.run();
				}

				// push the tuple token and set the state to tuple open
				this.addToken(TokenType.ARGUMENT_VALUE_TUPLE_START, this.tupleChars.first().toString());
				this.tupleOpen = true;

				// reached a possible tuple end character
			} else if (cChar == this.tupleChars.second()) {
				// if we are not in a tuple, set error and stop tokenizing
				if (!this.tupleOpen) {
					values.errorType = TokenizeError.TokenizeErrorType.UNEXPECTED_TUPLE_CLOSE;
					break;
				}

				// if there was something before the tuple, tokenize it
				if (!currentValue.isEmpty()) {
					this.addToken(TokenType.ARGUMENT_VALUE, currentValue.toString());
				}

				// push the tuple token and set the state to tuple closed
				this.addToken(TokenType.ARGUMENT_VALUE_TUPLE_END, this.tupleChars.second().toString());
				currentValue.setLength(0);
				this.tupleOpen = false;

				// reached a "--". Push all the rest as a FORWARD_VALUE.
			} else if (
				cChar == '-'
					&& this.isCharAtRelativeIndex(1, '-')
					&& this.isCharAtRelativeIndex(2, ' ')
			) {
				this.addToken(TokenType.FORWARD_VALUE, content.substring(this.currentCharIndex + 3));
				break;

				// reached a possible separator
			} else if (
				(cChar == ' ' && !currentValue.isEmpty()) // there's a space and some value to tokenize
					// also check if this is defining the value of an argument, or we are in a tuple. If so, don't tokenize
					|| (cChar == '=' && !this.tupleOpen && this.isArgumentSpecifier(currentValue.toString()))
			)
			{
				tokenizeSection.run();

				// push the current char to the current value
			} else if (cChar != ' ') {
				currentValue.append(cChar);
			}
		}

		if (values.errorType == null)
			if (this.tupleOpen) {
				values.errorType = TokenizeError.TokenizeErrorType.TUPLE_NOT_CLOSED;
			} else if (this.stringOpen) {
				values.errorType = TokenizeError.TokenizeErrorType.STRING_NOT_CLOSED;
			}

		// we left something in the current value, tokenize it
		if (!currentValue.isEmpty()) {
			tokenizeSection.run();
		}

		if (values.errorType != null) {
			this.addError(values.errorType, finalTokens.size());
		}

		this.finishedTokenizing = true;
	}

	private Token tokenizeSection(String str) {
		final TokenType type;

		if (this.tupleOpen || this.stringOpen) {
			type = TokenType.ARGUMENT_VALUE;
		} else if (this.isArgName(str)) {
			type = TokenType.ARGUMENT_NAME;
		} else if (this.isArgNameList(str)) {
			type = TokenType.ARGUMENT_NAME_LIST;
		} else if (this.isSubCommand(str)) {
			type = TokenType.SUB_COMMAND;
		} else {
			type = TokenType.ARGUMENT_VALUE;
		}

		return new Token(type, str);
	}

	public List<Command> getTokenizedSubCommands() {
		final List<Command> x = new ArrayList<>();
		final Command subCmd;

		x.add(this.command);
		if ((subCmd = this.getTokenizedSubCommand()) != null) {
			x.addAll(subCmd.getTokenizer().getTokenizedSubCommands());
		}
		return x;
	}

	private boolean isArgNameList(String str) {
		if (str.length() < 2) return false;

		final var possiblePrefixes = new ArrayList<Character>();
		final var charArray = str.substring(1).toCharArray();

		for (final char argName : charArray) {
			if (!runForArgument(argName, a -> possiblePrefixes.add(a.getPrefix())))
				break;
		}

		return possiblePrefixes.size() >= 1 && possiblePrefixes.contains(str.charAt(0));
	}

	private boolean isArgName(String str) {
		// first try to figure out if the prefix is used, to save time (does it start with '--'? (assuming the prefix is '-'))
		if (
			str.length() > 1 // make sure we are working with long enough strings
				&& str.charAt(0) == str.charAt(1) // first and second chars are equal?
		)
		{
			// now check if the name actually exist
			return this.command.getArguments().stream().anyMatch(a -> a.checkMatch(str));
		}

		return false;
	}

	private boolean isArgumentSpecifier(String str) {
		return this.isArgName(str) || this.isArgNameList(str);
	}

	private boolean isSubCommand(String str) {
		return this.getSubCommands().stream().anyMatch(c -> c.name.equals(str));
	}

	private Command getSubCommandByName(String name) {
		var x = this.getSubCommands().stream().filter(sc -> sc.name.equals(name)).toList();
		return x.isEmpty() ? null : x.get(0);
	}

	public Command getTokenizedSubCommand() {
		return this.getSubCommands().stream().filter(sb -> sb.getTokenizer().finishedTokenizing).findFirst().orElse(null);
	}

	public List<Token> getFinalTokens() {
		if (!this.finishedTokenizing) {
			throw new IllegalStateException("Cannot get final tokens before tokenizing is finished!");
		}
		return this.finalTokens;
	}

	public boolean isFinishedTokenizing() {
		return this.finishedTokenizing;
	}
}
