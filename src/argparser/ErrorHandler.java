package argparser;

import argparser.displayFormatter.Color;
import argparser.displayFormatter.FormatOption;
import argparser.displayFormatter.TextFormatter;
import argparser.utils.UtlString;

import java.util.ArrayList;
import java.util.List;

enum ParseErrorType {
	None,
	ArgumentNotFound,
	ObligatoryArgumentNotUsed,
	UnmatchedToken,
	ArgIncorrectValueNumber,
	CustomError;
}

enum TokenizeErrorType {
	None,
	TupleAlreadyOpen,
	UnexpectedTupleClose,
	TupleNotClosed,
	StringNotClosed
}

class ParseStateErrorBase<Type> {
	public final Type type;
	public final int index;

	public ParseStateErrorBase(Type type, int index) {
		this.type = type;
		this.index = index;
	}
}

class TokenizeError extends ParseStateErrorBase<TokenizeErrorType> {
	public TokenizeError(TokenizeErrorType type, int index) {
		super(type, index);
	}
}

class ParseError extends ParseStateErrorBase<ParseErrorType> {
	protected Argument<?, ?> argument;
	protected final int valueCount;

	public ParseError(ParseErrorType type, int index, Argument<?, ?> argument, int valueCount) {
		super(type, index);
		this.argument = argument;
		this.valueCount = valueCount;
	}

	void setArgument(Argument<?, ?> argument) {
		this.argument = argument;
	}

	public Argument<?, ?> arg() {
		return this.argument;
	}
}

class CustomParseError extends ParseError {
	public final String message;

	public CustomParseError(String message, int index) {
		super(ParseErrorType.CustomError, index, null, 0);
		this.message = message;
	}
}

public class ErrorHandler {
	private final Command rootCmd;
	private final List<Token> tokens;

	private int cmdAbsoluteTokenIndex = 0;

	private class ParseErrorHandlers {
		private int index;

		public void handleParseErrors(ArrayList<ParseError> errList) {
			var newList = new ArrayList<>(errList);
			for (var err : newList) {
				/* if we are going to show an error about an argument being incorrectly used, and that argument is defined
				 * as obligatory, we don't need to show the obligatory error since its obvious that the user knows that
				 * the argument is obligatory */
				if (err.type == ParseErrorType.ArgIncorrectValueNumber) {
					newList.removeIf(e -> e.arg().equals(err.arg()) && e.type == ParseErrorType.ObligatoryArgumentNotUsed);
				}
			}
			newList.forEach(this::handleError);
		}

		private void handleError(ParseError err) {
			this.index = err.index;

			formatErrorInfo(switch (err.type) {
				case ArgIncorrectValueNumber -> this.handleIncorrectValueNumber(err.arg(), err.valueCount);
				case ObligatoryArgumentNotUsed -> this.handleObligatoryArgumentNotUsed(err.arg());
				case ArgumentNotFound -> this.handleArgumentNotFound(tokens.get(this.index).contents());

				default -> {
					displayTokensWithError(err.index);
					yield err.type.toString();
				}
			});
		}

		private String handleIncorrectValueNumber(Argument<?, ?> arg, int valueCount) {
			displayTokensWithError(this.index + 1, valueCount, valueCount == 0);
			return String.format(
				"Incorrect number of values for argument '%s'.%nExpected %s, but got %d.",
				arg.getAlias(), arg.getNumberOfValues().getMessage(), Math.max(valueCount - 1, 0)
			);
		}

		private String handleObligatoryArgumentNotUsed(Argument<?, ?> arg) {
			displayTokensWithError(this.index);
			var argCmd = arg.getParentCmd();
			return argCmd.isRootCommand()
				? String.format("Obligatory argument '%s' not used.", arg.getAlias())
				: String.format("Obligatory argument '%s' for command '%s' not used.", arg.getAlias(), argCmd.name);
		}

		private String handleArgumentNotFound(String argName) {
			return "Argument '" + argName + "' not found.";
		}
	}

	public ErrorHandler(Command cmd) {
		this.rootCmd = cmd;
		this.tokens = cmd.getFullTokenList();
	}

	private void formatErrorInfo(String contents) {
		// first figure out the length of the longest line
		var maxLength = UtlString.getLongestLine(contents).length();

		var formatter = new TextFormatter()
			.setColor(Color.BrightRed)
			.addFormat(FormatOption.Bold);

		System.err.println(
			contents.replaceAll(
				"^|\\n",
				formatter.setContents("\n │ ").toString() // first insert a vertical bar at the start of each
				// line
			)
				// then insert a horizontal bar at the end, with the length of the longest line
				// approximately
				+ formatter.setContents("\n └" + "─".repeat(Math.max(maxLength - 5, 0)) + " ───── ── ─")
				.toString()
				+ "\n");
	}

	private void displayTokensWithError(int start, int offset, boolean placeArrow) {
		start += this.cmdAbsoluteTokenIndex;
		final var arrow = TextFormatter.ERROR("<-");
		var tokensFormatters = new ArrayList<>(this.tokens.stream().map(Token::getFormatter).toList());
		int tokensLength = this.tokens.size();

		if (start < 0) {
			tokensFormatters.add(0, arrow);
		} else if (start >= tokensLength) {
			tokensFormatters.add(arrow);
		}

		for (int i = 0; i < tokensLength; i++) {
			if (i < this.cmdAbsoluteTokenIndex) {
				tokensFormatters.get(i).addFormat(FormatOption.Dim);
			}

			if (i >= start && i < start + offset + 1) {
				if (placeArrow) {
					tokensFormatters.add(i + 1, arrow);
				} else {
					tokensFormatters.get(i)
						.setColor(Color.BrightRed)
						.addFormat(FormatOption.Reverse, FormatOption.Bold);
				}
			}
		}

		System.err.print(String.join(" ", tokensFormatters.stream().map(TextFormatter::toString).toList()));
	}

	private void displayTokensWithError(int index) {
		this.displayTokensWithError(index, 0, true);
	}

	public void handleErrors() {
		var parseErrorHandler = this.new ParseErrorHandlers();
		List<Command> commands = this.rootCmd.getTokenizedSubCommands();

		for (int i = 0; i < commands.size(); i++) {
			Command cmd = commands.get(i);
			this.cmdAbsoluteTokenIndex = getCommandTokenIndexByNestingLevel(i);

			for (var tokenizeError : cmd.tokenizeState.errors) {
				System.out.println(tokenizeError.type);
			}

			parseErrorHandler.handleParseErrors(cmd.parseState.errors);
		}
	}

	private int getCommandTokenIndexByNestingLevel(int level) {
		for (int i = 0, appearances = 0; i < this.tokens.size(); i++) {
			if (this.tokens.get(i).type() == TokenType.SubCommand) {
				appearances++;
			}
			if (appearances >= level) {
				return i - (level == 0 ? 1 : 0); // this is done to skip the subcommand token itself
			}
		}
		return -1;
	}

	public int getErrorCode() {
		// TODO: implement error codes
		return 1;
	}

	public boolean hasErrors() {
		return this.rootCmd.getTokenizedSubCommands().stream().anyMatch(cmd -> !cmd.parseState.errors.isEmpty());
	}
}