package lanat;

import org.jetbrains.annotations.NotNull;

/**
 * A class to gather the input from the command line.
 */
public final class CLInput {
	/**
	 * The string of arguments passed to the program.
	 */
	public final @NotNull String args;

	private CLInput(@NotNull String args) {
		this.args = args.trim();
	}

	/**
	 * Constructs a new {@link CLInput} from the given arguments array.
	 * @param args The array of arguments.
	 * @return A new {@link CLInput} from the given arguments array.
	 */
	public static @NotNull CLInput from(@NotNull String @NotNull [] args) {
		return new CLInput(String.join(" ", args));
	}

	/**
	 * Constructs a new {@link CLInput} from the given arguments string.
	 * @param args The arguments string.
	 * @return A new {@link CLInput} from the given arguments string.
	 */
	public static @NotNull CLInput from(@NotNull String args) {
		return new CLInput(args);
	}

	/**
	 * Gets the arguments passed to the program from the system property {@code "sun.java.command"}.
	 * @return A new {@link CLInput} from the system property {@code "sun.java.command"}.
	 */
	public static @NotNull CLInput fromSystemProperty() {
		final var args = System.getProperty("sun.java.command");

		// remove first word from args (the program name)
		return new CLInput(args.substring(args.indexOf(' ') + 1));
	}

	/** Returns {@code true} if no arguments were passed to the program. */
	public boolean isEmpty() {
		return this.args.isEmpty();
	}
}