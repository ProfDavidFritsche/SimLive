package simlive.model;

import java.util.Arrays;

import simlive.SimLive;

public class TimeTable implements DeepEqualsInterface {

	private double[] time;
	private double[] factor;
	
	public TimeTable(double[] time, double[] factor) {
		this.time = time;
		this.factor = factor;
	}
	
	public TimeTable() {
		time = new double[2];
		factor = new double[2];
		time[1] = SimLive.model.getTotalDuration();
		factor[1] = 1.0;
	}

	public TimeTable clone() {
		TimeTable timeTable = new TimeTable();
		timeTable.time = Arrays.copyOf(this.time, this.time.length);
		timeTable.factor = Arrays.copyOf(this.factor, this.factor.length);
		return timeTable;
	}
	
	public boolean deepEquals(Object obj) {
		TimeTable timeTable = (TimeTable) obj;
		if (!Arrays.equals(this.time, timeTable.time)) return false;
		if (!Arrays.equals(this.factor, timeTable.factor)) return false;
		return true;
	}
	
	public int getNumberOfRows() {
		return time.length;
	}
	
	public double getTime(int i) {
		return time[i];
	}
	
	public double getFactor(int i) {
		return factor[i];
	}
	
	public void setTime(double time, int i) {
		this.time[i] = time;
	}
	
	public void setFactor(double factor, int i) {
		this.factor[i] = factor;
	}
	
	public void setNumberOfRows(int nr) {
		time = Arrays.copyOf(time, nr);
		factor = Arrays.copyOf(factor, nr);
	}
	
	public boolean isFactorDefinedAtTime(double t) {
		return (t >= time[0] && t <= time[time.length-1]) || t < 0.0;
	}
	
	public double getFactorAtTime(double t) {
		if (t < 0.0) return 1.0;
		if (getNumberOfRows() == 1) {
			if (time[0] == t) {
				return factor[0];
			}
		}
		for (int i = 1; i < time.length; i++) {
			if (time[i] > t || i == time.length-1) {
				return factor[i-1]+(factor[i]-factor[i-1])/(time[i]-time[i-1])*(t-time[i-1]);
			}
		}
		return 0.0;
	}

}
