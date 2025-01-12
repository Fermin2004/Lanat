package lanat.argumentTypes;

import lanat.NamedWithDescription;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import textFormatter.TextFormatter;
import utils.Range;

/**
 * The basic interface for all argument types. In order to use a class that implements this interface as an
 * argument type, you must use {@link FromParseableArgumentType} to wrap it.
 * @param <T> The type that this argument type parses.
 * @see FromParseableArgumentType
 */
public interface Parseable<T> extends NamedWithDescription {
	/** Specifies the number of values that this parser should receive when calling {@link #parseValues(String[])}. */
	@NotNull Range getRequiredArgValueCount();

	/**
	 * Parses the received values and returns the result. If the values are invalid, this method shall return {@code null}.
	 *
	 * @param args The values that were received.
	 * @return The parsed value.
	 */
	@Nullable T parseValues(@NotNull String... args);

	/** Returns the representation of this parseable type. This may appear in places like the help message. */
	default @Nullable TextFormatter getRepresentation() {
		return new TextFormatter(this.getName());
	}

	@Override
	default @NotNull String getName() {
		// Remove the "ArgumentType" suffix from the class name
		return this.getClass().getSimpleName().replaceAll("ArgumentType$", "");
	}

	@Override
	default @Nullable String getDescription() {
		return null;
	}
}
