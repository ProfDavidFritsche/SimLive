package simlive.model;

public interface DeepEqualsInterface {
	public enum Result {EQUAL, CHANGE, RECALC}
	public Result deepEquals(Object obj, Result result);
}
