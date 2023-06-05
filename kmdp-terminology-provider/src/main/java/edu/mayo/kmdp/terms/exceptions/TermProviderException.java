package edu.mayo.kmdp.terms.exceptions;

public class TermProviderException extends RuntimeException {

  public TermProviderException() {
    super("Unable to read the JSON file which leaves the application in unstable state.");
  }

}
