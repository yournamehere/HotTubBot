import java.io.*;
import java.util.*;

public class HighNoon {
	
	public ArrayList<City> cities = new ArrayList<City>();
	
	public static void main (String[] args)  throws FileNotFoundException, IOException{
		HighNoon highnoon = new HighNoon();
		highnoon.getHighNoonLocation();
	}
	public HighNoon() throws FileNotFoundException, IOException{
		FileReader file = new FileReader("world_cities.csv");
		BufferedReader in = new BufferedReader(file);
		String line = in.readLine(); //line 1 is a description of the fields
		while ((line = in.readLine()) != null) {
			String[] elements = line.split(",");
			String city = elements[0];
			//sometimes province is empty, and split doesn't add an empty element
			String province = "";
			if(elements.length>8){
				province = elements[8];
			}
			String country = elements[5];
			//sometimes places have fractional population for some reason.
			int population  = (int)Double.parseDouble(elements[4]);
			double longitude = Double.parseDouble(elements[3]);
			cities.add(new City(city, province, country, population, longitude));
		}
		Collections.sort(cities);
	}
	public String getHighNoonLocation(){
		double longitude = getNoonLongitude();
		System.out.println(longitude);
		String retval = getNearest(longitude).toString();
		System.out.println(retval);
		return retval;
	}
	double getNoonLongitude(){
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		int miliseconds = calendar.get(Calendar.MILLISECOND);
		miliseconds += 1000*calendar.get(Calendar.SECOND);
		miliseconds += 60*1000*calendar.get(Calendar.MINUTE);
		miliseconds += 60*60*1000*calendar.get(Calendar.HOUR_OF_DAY);
		int max = 24*60*60*1000;
		double scalefactor = 360f/(float)max;
		
		return -((miliseconds*scalefactor) - 180f);
	}
	City getNearest(double longitude){
		int index = Collections.binarySearch(cities, new City(longitude));
		if(index < 0){
			//we got where our coordinate would be inserted, so now we just have to find the nearest of two neighbors
			int rightindex = -index -1;
			int leftindex = rightindex -1;
			double rightdist = Math.abs(longitude - cities.get(rightindex).longitude);
			double leftdist = Math.abs(longitude - cities.get(leftindex).longitude);
			if (rightdist > leftdist){
				index = rightindex;
			}
			else {
				index = leftindex;
			}
		}
		return cities.get(index);
	}
	
	class City implements Comparable<City>{
	
		public String name;
		public String province;
		public String country;
		public int population;
		public Double longitude;
		
		public City(double l){
			this("","","",0,l);
		}
		
		public City(String n, String p, String c, int pop, double l){
			name = n;
			province = p;
			country = c;
			population = pop;
			longitude = l;
		}
		
		public int compareTo (City toCompare){
			return this.longitude.compareTo(toCompare.longitude);
		}
		
		public String toString(){
			String pre = "It is currently high noon in ";
			if (province.equals("")){
				return pre + name + ", " + country;
			}
				return pre + name + ", " + province +", " + country;
		}
	
	}
}