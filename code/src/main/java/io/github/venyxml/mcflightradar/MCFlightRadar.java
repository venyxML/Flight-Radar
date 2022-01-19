package io.github.venyxml.mcflightradar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.opensky.api.OpenSkyApi;
import org.opensky.model.OpenSkyStates;
import org.opensky.model.StateVector;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;

public final class MCFlightRadar extends JavaPlugin {


    @Override
    public void onEnable() {
        getLogger().info("Flight Radar initializing...");
        new ControlCommand(this);
        this.getCommand("fr").setExecutor(new ControlCommand(this));
    }

    @Override
    public void onDisable() {
        getLogger().info("Flight Radar disabling...");
    }
}

class ControlCommand implements CommandExecutor {

    static double oLat = 0;
    static double oLon = 0;

    MCFlightRadar flightradar;
    static int task;
    static int task2;

    static ArrayList<String> lastCallsigns = new ArrayList<>();

    ControlCommand(MCFlightRadar flightradar){ this.flightradar = flightradar; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length > 0){
            if(args[0].equalsIgnoreCase("start") ){
                if(args.length == 3){
                   try{
                       Player p = (Player) sender;

                       oLat = Double.parseDouble(args[1]);
                       oLon = Double.parseDouble(args[2]);

                       task = Bukkit.getScheduler().scheduleSyncRepeatingTask(flightradar, new Runnable() {

                           @Override
                           public void run() {
                               try {
                                   double[] bounds = getBoundingBox(oLat,oLon);
                                   //p.sendMessage("Bounding Box: " + bounds[0] + ", " + bounds[1] + ", " + bounds[2] + ", " + bounds[3]);
                                   checkFlights(p, bounds);
                               } catch (IOException e) {
                                   System.out.println("[FlightRadar] Error in checking flight statuses.");
                               }

                           }
                       }, 0, 20*10);

                       task2 = Bukkit.getScheduler().scheduleSyncRepeatingTask(flightradar, new Runnable() {

                           @Override
                           public void run() {
                               try {
                                   lastCallsigns.clear();
                               } catch (Exception e) {
                                   System.out.println("[FlightRadar] Error in clearing remembered flights.");
                               }

                           }
                       }, 0, 20*60*30);

                       return true;
                   }
                   catch(Exception e){
                       return false;
                   }
                }
            }
            else if(args[0].equalsIgnoreCase("stop")){
                try{
                    Bukkit.getScheduler().cancelTask(task);
                    Bukkit.getScheduler().cancelTask(task2);
                    return true;
                } catch(Exception e){
                    System.out.println("[FlightRadar] Error in cancelling task.");
                }

            } else if(args[0].equalsIgnoreCase("clear")){
                try{
                    lastCallsigns.clear();
                } catch(Exception e){
                    System.out.println("[FlightRadar] Error in clearing remembered flights.");
                }
            }
        }

        return false;
    }

    public void checkFlights(Player p, double[] bounds) throws IOException {
        OpenSkyApi api = new OpenSkyApi();
        OpenSkyStates os = api.getStates(0, null,
                new OpenSkyApi.BoundingBox(bounds[0], bounds[1], bounds[2], bounds[3]));

        Collection<StateVector> flights = os.getStates();

        if(flights != null){
            for(StateVector flight : flights){
                if(flight.getCallsign() != null && !flight.getCallsign().equalsIgnoreCase("") && !lastCallsigns.contains(flight.getCallsign()) && !flight.isOnGround()){ //flights listed with callsign
                    p.sendMessage(ChatColor.RESET + "Aircraft " + ChatColor.BOLD + "" + ChatColor.GRAY + flight.getCallsign() + ChatColor.RESET + "is flying over you!");
                    if(flight.getSquawk() != null && (flight.getSquawk().equalsIgnoreCase("7500") || flight.getSquawk().equalsIgnoreCase("7600") || flight.getSquawk().equalsIgnoreCase("7700"))){
                        p.sendMessage(ChatColor.DARK_RED + "This flight has declared an emergency with Squawk " + ChatColor.BOLD + flight.getSquawk());
                    }
                    lastCallsigns.add(flight.getCallsign());
                } else if (flight.getCallsign() == null && !lastCallsigns.contains("") && !flight.isOnGround()){ //no callsign
                    p.sendMessage(ChatColor.RESET + "An unidentified aircraft is flying over you!");
                    if(flight.getSquawk() != null && (flight.getSquawk().equalsIgnoreCase("7500") || flight.getSquawk().equalsIgnoreCase("7600") || flight.getSquawk().equalsIgnoreCase("7700"))){
                        p.sendMessage(ChatColor.DARK_RED + "This flight has declared an emergency with Squawk " + ChatColor.BOLD + flight.getSquawk());
                    }
                    lastCallsigns.add("");
                }
            } //loop thru flights
        } //check null

    }

    private double[] getBoundingBox(double lat, double lon){
        double r = 5; // miles, change to config later
        double dLat = r/69; //only used for small approximations
        double dLon = dLat/Math.cos(lat*(Math.PI/180));

        double[] bounds = new double[4];

        bounds[0] = lat - dLat;
        bounds[1] = lat + dLat;
        bounds[2] = lon - dLon;
        bounds[3] = lon + dLon;

        return bounds;
    }

}