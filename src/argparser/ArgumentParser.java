package argparser;

import argparser.displayFormatter.ErrorHandler;

public class ArgumentParser extends Command {
	public ArgumentParser(String programName, String description) {
		super(programName, description);
	}

	public ArgumentParser(String programName) {
		this(programName, null);
	}


	public ParsedArguments parseArgs(String[] args) {
		// if we receive the classic args array, just join it back
		return this.parseArgs(String.join(" ", args));
	}

	public ParsedArguments parseArgs(String args) {
		var res = this.tokenize(args); // first. This will tokenize all subCommands recursively
		var errorHandler = new ErrorHandler(this.getFullTokenList());

		errorHandler.handleTokensResult(res);
		errorHandler.displayTokens();

		var res2 = this.parseTokens(); // same thing, this parses all the stuff recursively

		return new ParsedArguments(null, null, null);
	}

	public ArgumentParser tupleCharacter(TupleCharacter tupleCharacter) {
		this.tupleChars = tupleCharacter.getCharPair();
		return this;
	}
}