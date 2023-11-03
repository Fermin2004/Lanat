package lanat.parsing.errors;

import lanat.ErrorLevel;
import lanat.utils.ErrorLevelProvider;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class TokenizeError extends ParseStateErrorBase<TokenizeError.TokenizeErrorType> {
	public enum TokenizeErrorType implements ErrorLevelProvider {
		TUPLE_ALREADY_OPEN,
		UNEXPECTED_TUPLE_CLOSE,
		TUPLE_NOT_CLOSED,
		STRING_NOT_CLOSED;

		@Override
		public @NotNull ErrorLevel getErrorLevel() {
			return ErrorLevel.ERROR;
		}
	}

	public TokenizeError(@NotNull TokenizeErrorType type, int index) {
		super(type, index);
	}

	@Handler("TUPLE_ALREADY_OPEN")
	protected void handleTupleAlreadyOpen() {
		this.fmt()
			.setContent("Tuple already open.")
			.displayTokens(this.tokenIndex, 0, false);
	}

	@Handler("TUPLE_NOT_CLOSED")
	protected void handleTupleNotClosed() {
		this.fmt()
			.setContent("Tuple not closed.")
			.displayTokens(this.tokenIndex + 1);
	}

	@Handler("UNEXPECTED_TUPLE_CLOSE")
	protected void handleUnexpectedTupleClose() {
		this.fmt()
			.setContent("Unexpected tuple close.")
			.displayTokens(this.tokenIndex, 0, false);
	}

	@Handler("STRING_NOT_CLOSED")
	protected void handleStringNotClosed() {
		this.fmt()
			.setContent("String not closed.")
			.displayTokens(this.tokenIndex + 1);
	}
}
