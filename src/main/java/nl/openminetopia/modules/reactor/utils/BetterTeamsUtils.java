package nl.openminetopia.modules.reactor.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

/**
 * Utility class for BetterTeams integration
 * Uses reflection to access BetterTeams API
 */
public class BetterTeamsUtils {

    private static Object betterTeamsPlugin;
    private static Object teamManager;
    private static boolean initialized = false;
    private static boolean debugMode = false; // Enable for detailed logging

    static {
        initialize();
    }

    /**
     * Re-initializes BetterTeams connection (useful if plugin loads after this class)
     */
    public static void reinitialize() {
        initialized = false;
        teamManager = null;
        betterTeamsPlugin = null;
        initialize();
    }

    /**
     * Enables or disables debug logging
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    /**
     * Checks if BetterTeams is initialized
     */
    public static boolean isInitialized() {
        return initialized && teamManager != null;
    }

    private static void initialize() {
        // Check if BetterTeams is enabled
        if (!Bukkit.getPluginManager().isPluginEnabled("BetterTeams")) {
            Bukkit.getLogger().warning("[ReactorModule] BetterTeams plugin is not enabled! Make sure BetterTeams is installed and enabled.");
            initialized = false;
            return;
        }

        Plugin plugin = Bukkit.getPluginManager().getPlugin("BetterTeams");
        if (plugin == null) {
            Bukkit.getLogger().warning("[ReactorModule] BetterTeams plugin not found! Make sure BetterTeams is installed and enabled.");
            initialized = false;
            return;
        }
        
        // Additional check: ensure plugin is actually loaded
        if (!plugin.isEnabled()) {
            Bukkit.getLogger().warning("[ReactorModule] BetterTeams plugin is not enabled yet! Waiting for it to load...");
            initialized = false;
            return;
        }

        betterTeamsPlugin = plugin;
        Bukkit.getLogger().info("[ReactorModule] Found BetterTeams plugin: " + plugin.getClass().getName());
        
        try {
            // BetterTeams uses a different structure - try multiple approaches
            Object manager = null;
            String methodUsed = null;
            
            // Method 1: Try getTeamManager() first (standard API)
            try {
                manager = plugin.getClass().getMethod("getTeamManager").invoke(plugin);
                methodUsed = "getTeamManager()";
            } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
                // Method 2: Try to access via fields (TeamManager might be a private field)
                try {
                    java.lang.reflect.Field teamManagerField = null;
                    // Try common field names
                    for (String fieldName : new String[]{"teamManager", "manager", "teamManagerInstance"}) {
                        try {
                            teamManagerField = plugin.getClass().getDeclaredField(fieldName);
                            break;
                        } catch (NoSuchFieldException ignored) {
                        }
                    }
                    
                    // If not found, search all fields
                    if (teamManagerField == null) {
                        for (java.lang.reflect.Field field : plugin.getClass().getDeclaredFields()) {
                            String fieldName = field.getName().toLowerCase();
                            String fieldType = field.getType().getName().toLowerCase();
                            if ((fieldName.contains("team") && fieldName.contains("manager")) || 
                                fieldType.contains("teammanager")) {
                                teamManagerField = field;
                                break;
                            }
                        }
                    }
                    
                    if (teamManagerField != null) {
                        teamManagerField.setAccessible(true);
                        manager = teamManagerField.get(plugin);
                        methodUsed = "field: " + teamManagerField.getName();
                    }
                } catch (Exception ex) {
                    // Continue to next method
                }
                
                // Method 2b: Try via teamManagement field (MCTeamManagement might have TeamManager)
                if (manager == null) {
                    try {
                        java.lang.reflect.Field teamManagementField = plugin.getClass().getDeclaredField("teamManagement");
                        teamManagementField.setAccessible(true);
                        Object teamManagement = teamManagementField.get(plugin);
                        
                        if (teamManagement != null) {
                            // Log MCTeamManagement methods for debugging
                            Bukkit.getLogger().info("[ReactorModule] MCTeamManagement methods:");
                            for (java.lang.reflect.Method method : teamManagement.getClass().getMethods()) {
                                String methodName = method.getName().toLowerCase();
                                if (methodName.contains("team") || methodName.contains("manager") || methodName.contains("get")) {
                                    Bukkit.getLogger().info("[ReactorModule]   - " + method.getName() + "(" + 
                                        java.util.Arrays.toString(method.getParameterTypes()) + ")");
                                }
                            }
                            Bukkit.getLogger().info("[ReactorModule] MCTeamManagement fields:");
                            for (java.lang.reflect.Field field : teamManagement.getClass().getDeclaredFields()) {
                                Bukkit.getLogger().info("[ReactorModule]   - " + field.getName() + " (" + field.getType().getName() + ")");
                            }
                            
                            // Try to get TeamManager from MCTeamManagement
                            try {
                                manager = teamManagement.getClass().getMethod("getTeamManager").invoke(teamManagement);
                                methodUsed = "teamManagement.getTeamManager()";
                            } catch (NoSuchMethodException ex) {
                                // Try field access
                                for (java.lang.reflect.Field field : teamManagement.getClass().getDeclaredFields()) {
                                    String fieldType = field.getType().getName().toLowerCase();
                                    if (fieldType.contains("teammanager")) {
                                        field.setAccessible(true);
                                        manager = field.get(teamManagement);
                                        methodUsed = "teamManagement.field: " + field.getName();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Continue
                    }
                }
                
                // Method 2c: Try to access TeamManager via static method or singleton pattern
                if (manager == null) {
                    try {
                        // Try to find TeamManager class and get instance
                        Class<?>[] classesToTry = {
                            Class.forName("com.booksaw.betterTeams.TeamManager"),
                            Class.forName("com.booksaw.betterTeams.team.TeamManager"),
                            Class.forName("com.booksaw.betterTeams.managers.TeamManager")
                        };
                        
                        for (Class<?> teamManagerClass : classesToTry) {
                            try {
                                // Try getInstance() method
                                try {
                                    java.lang.reflect.Method getInstance = teamManagerClass.getMethod("getInstance");
                                    if (java.lang.reflect.Modifier.isStatic(getInstance.getModifiers())) {
                                        manager = getInstance.invoke(null);
                                        methodUsed = teamManagerClass.getName() + ".getInstance()";
                                        break;
                                    }
                                } catch (NoSuchMethodException ex9) {
                                    // Try other static methods
                                    for (java.lang.reflect.Method method : teamManagerClass.getMethods()) {
                                        if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && 
                                            method.getReturnType().equals(teamManagerClass) && 
                                            method.getParameterCount() == 0) {
                                            manager = method.invoke(null);
                                            methodUsed = teamManagerClass.getName() + "." + method.getName() + "()";
                                            break;
                                        }
                                    }
                                    if (manager != null) break;
                                }
                                
                                // Try static field
                                for (java.lang.reflect.Field field : teamManagerClass.getDeclaredFields()) {
                                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                                        field.getType().equals(teamManagerClass)) {
                                        field.setAccessible(true);
                                        manager = field.get(null);
                                        methodUsed = teamManagerClass.getName() + ".static field: " + field.getName();
                                        break;
                                    }
                                }
                                if (manager != null) break;
                            } catch (Exception ex11) {
                                // Try next class
                            }
                        }
                    } catch (Exception ex12) {
                        // Continue
                    }
                }
                
                // Method 3: Try getLifecycleManager() and access TeamManager from there
                if (manager == null) {
                    try {
                        Object lifecycleManager = plugin.getClass().getMethod("getLifecycleManager").invoke(plugin);
                        if (lifecycleManager != null) {
                            // Try to get TeamManager from LifecycleManager via method
                            try {
                                manager = lifecycleManager.getClass().getMethod("getTeamManager").invoke(lifecycleManager);
                                methodUsed = "getLifecycleManager().getTeamManager()";
                            } catch (Exception ex2) {
                                // Try field access on LifecycleManager
                                try {
                                    for (java.lang.reflect.Field field : lifecycleManager.getClass().getDeclaredFields()) {
                                        String fieldName = field.getName().toLowerCase();
                                        String fieldType = field.getType().getName().toLowerCase();
                                        if ((fieldName.contains("team") && fieldName.contains("manager")) || 
                                            fieldType.contains("teammanager")) {
                                            field.setAccessible(true);
                                            manager = field.get(lifecycleManager);
                                            methodUsed = "getLifecycleManager().field: " + field.getName();
                                            break;
                                        }
                                    }
                                } catch (Exception ex3) {
                                    // Continue
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Continue
                    }
                }
                
                // Method 4: Try to find TeamManager class directly via static methods
                if (manager == null) {
                    try {
                        // Look for TeamManager class in the plugin's package
                        Class<?> teamManagerClass = Class.forName("com.booksaw.betterTeams.TeamManager");
                        // Try to get instance via static method
                        try {
                            java.lang.reflect.Method getInstance = teamManagerClass.getMethod("getInstance");
                            manager = getInstance.invoke(null);
                            methodUsed = "TeamManager.getInstance()";
                        } catch (NoSuchMethodException ex4) {
                            // Try other static methods
                            for (java.lang.reflect.Method method : teamManagerClass.getMethods()) {
                                if (java.lang.reflect.Modifier.isStatic(method.getModifiers()) && 
                                    method.getReturnType().equals(teamManagerClass) && 
                                    method.getParameterCount() == 0) {
                                    manager = method.invoke(null);
                                    methodUsed = "TeamManager." + method.getName() + "()";
                                    break;
                                }
                            }
                        }
                    } catch (ClassNotFoundException ex5) {
                        // TeamManager class not found - try alternative class names
                        try {
                            Class<?> teamManagerClass = Class.forName("com.booksaw.betterTeams.team.TeamManager");
                            try {
                                java.lang.reflect.Method getInstance = teamManagerClass.getMethod("getInstance");
                                manager = getInstance.invoke(null);
                                methodUsed = "TeamManager.getInstance() (alternative package)";
                            } catch (NoSuchMethodException ex6) {
                                // Try field access on the class itself
                                for (java.lang.reflect.Field field : teamManagerClass.getDeclaredFields()) {
                                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                                        field.getType().equals(teamManagerClass)) {
                                        field.setAccessible(true);
                                        manager = field.get(null);
                                        methodUsed = "TeamManager.static field: " + field.getName();
                                        break;
                                    }
                                }
                            }
                        } catch (ClassNotFoundException ex7) {
                            // Alternative class also not found
                        } catch (Exception ex8) {
                            // Other error
                        }
                    } catch (Exception ex6) {
                        // Other error
                    }
                }
                
                if (manager == null) {
                    // List all available methods and fields for debugging
                    Bukkit.getLogger().warning("[ReactorModule] Could not find TeamManager. Available methods:");
                    for (java.lang.reflect.Method method : plugin.getClass().getMethods()) {
                        String methodName = method.getName().toLowerCase();
                        if (methodName.contains("team") || methodName.contains("manager") || methodName.contains("lifecycle")) {
                            Bukkit.getLogger().warning("[ReactorModule]   Method: " + method.getName() + "(" + 
                                java.util.Arrays.toString(method.getParameterTypes()) + ")");
                        }
                    }
                    Bukkit.getLogger().warning("[ReactorModule] Available fields (all):");
                    for (java.lang.reflect.Field field : plugin.getClass().getDeclaredFields()) {
                        Bukkit.getLogger().warning("[ReactorModule]   Field: " + field.getName() + " (" + field.getType().getName() + ")");
                    }
                    
                    // Also check LifecycleManager fields if available
                    try {
                        Object lifecycleManager = plugin.getClass().getMethod("getLifecycleManager").invoke(plugin);
                        if (lifecycleManager != null) {
                            Bukkit.getLogger().warning("[ReactorModule] LifecycleManager fields:");
                            for (java.lang.reflect.Field field : lifecycleManager.getClass().getDeclaredFields()) {
                                Bukkit.getLogger().warning("[ReactorModule]   Field: " + field.getName() + " (" + field.getType().getName() + ")");
                            }
                            Bukkit.getLogger().warning("[ReactorModule] LifecycleManager methods:");
                            for (java.lang.reflect.Method method : lifecycleManager.getClass().getMethods()) {
                                String methodName = method.getName().toLowerCase();
                                if (methodName.contains("team") || methodName.contains("manager") || methodName.contains("get")) {
                                    Bukkit.getLogger().warning("[ReactorModule]   Method: " + method.getName() + "(" + 
                                        java.util.Arrays.toString(method.getParameterTypes()) + ")");
                                }
                            }
                        }
                    } catch (Exception ex7) {
                        // Ignore
                    }
                    
                    throw new RuntimeException("Could not find TeamManager in BetterTeams plugin");
                }
            }
            
            if (manager == null) {
                throw new RuntimeException("TeamManager is null after all attempts");
            }
            
            // Log all methods of the manager for debugging
            if (debugMode) {
                Bukkit.getLogger().info("[ReactorModule] Manager class: " + manager.getClass().getName());
                Bukkit.getLogger().info("[ReactorModule] Manager methods:");
                for (java.lang.reflect.Method method : manager.getClass().getMethods()) {
                    Bukkit.getLogger().info("[ReactorModule]   - " + method.getName() + "(" + 
                        java.util.Arrays.toString(method.getParameterTypes()) + ")");
                }
            }
            
            // Verify the manager has the expected methods - try multiple method names
            boolean hasGetTeam = false;
            boolean hasGetTeams = false;
            
            // Check for getTeam method (various signatures)
            for (java.lang.reflect.Method method : manager.getClass().getMethods()) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.equals("getteam") || methodName.equals("getteambyuuid") || methodName.equals("findteam")) && 
                    method.getParameterCount() == 1) {
                    hasGetTeam = true;
                    break;
                }
            }
            
            // Check for getTeams method (various names)
            for (java.lang.reflect.Method method : manager.getClass().getMethods()) {
                String methodName = method.getName().toLowerCase();
                if ((methodName.equals("getteams") || methodName.equals("getallteams") || methodName.equals("getteamlist")) && 
                    method.getParameterCount() == 0) {
                    hasGetTeams = true;
                    break;
                }
            }
            
            if (!hasGetTeam || !hasGetTeams) {
                Bukkit.getLogger().warning("[ReactorModule] TeamManager found but missing expected methods. Available methods:");
                for (java.lang.reflect.Method method : manager.getClass().getMethods()) {
                    String methodName = method.getName().toLowerCase();
                    if (methodName.contains("team") || methodName.contains("get")) {
                        Bukkit.getLogger().warning("[ReactorModule]   - " + method.getName() + "(" + 
                            java.util.Arrays.toString(method.getParameterTypes()) + ")");
                    }
                }
                throw new RuntimeException("TeamManager does not have expected methods (getTeam, getTeams)");
            }
            
            teamManager = manager;
            initialized = true;
            Bukkit.getLogger().info("[ReactorModule] BetterTeams initialized successfully! Method: " + methodUsed + ", TeamManager: " + manager.getClass().getName());
        } catch (Exception e) {
            initialized = false;
            Bukkit.getLogger().severe("[ReactorModule] Failed to initialize BetterTeams: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
        }
    }

    private static void debug(String message) {
        if (debugMode) {
            Bukkit.getLogger().info("[ReactorModule] " + message);
        }
    }

    /**
     * Gets the team UUID for a player
     */
    public static UUID getPlayerTeamId(Player player) {
        if (!initialized || teamManager == null || player == null) {
            if (debugMode && player != null) {
                debug("getPlayerTeamId: Not initialized or player is null for " + player.getName());
            }
            return null;
        }

        try {
            // Try getTeam(OfflinePlayer) first
            Object team = teamManager.getClass()
                    .getMethod("getTeam", org.bukkit.OfflinePlayer.class)
                    .invoke(teamManager, player);
            
            if (team != null) {
                UUID teamId = (UUID) team.getClass().getMethod("getId").invoke(team);
                debug("getPlayerTeamId: Found team " + teamId + " for player " + player.getName());
                return teamId;
            }
        } catch (Exception e) {
            // Try alternative method: getTeam(UUID)
            try {
                Object team = teamManager.getClass()
                        .getMethod("getTeam", UUID.class)
                        .invoke(teamManager, player.getUniqueId());
                
                if (team != null) {
                    UUID teamId = (UUID) team.getClass().getMethod("getId").invoke(team);
                    debug("getPlayerTeamId: Found team " + teamId + " for player " + player.getName() + " (via UUID)");
                    return teamId;
                }
            } catch (Exception ex) {
                // BetterTeams API not available or changed
                Bukkit.getLogger().warning("[ReactorModule] Failed to get team for player " + player.getName() + ": " + ex.getMessage());
                if (debugMode) {
                    ex.printStackTrace();
                }
            }
        }
        
        if (debugMode) {
            debug("getPlayerTeamId: No team found for player " + player.getName());
        }
        return null;
    }

    /**
     * Gets the team name for a team UUID
     */
    public static String getTeamName(UUID teamId) {
        if (!initialized || teamManager == null || teamId == null) {
            return null;
        }

        try {
            Object team = teamManager.getClass()
                    .getMethod("getTeam", UUID.class)
                    .invoke(teamManager, teamId);
            
            if (team == null) {
                return null;
            }

            // Team.getName() returns String
            return (String) team.getClass().getMethod("getName").invoke(team);
        } catch (Exception e) {
            // Log warning for debugging
            if (Bukkit.getPluginManager().isPluginEnabled("BetterTeams")) {
                Bukkit.getLogger().warning("[ReactorModule] Failed to get team name for " + teamId + ": " + e.getMessage());
            }
            return null;
        }
    }

    /**
     * Gets the number of online members in a team (worldwide)
     * Counts all online team members across all worlds
     */
    public static int getOnlineMemberCount(UUID teamId) {
        if (!initialized || teamManager == null || teamId == null) {
            if (debugMode) {
                debug("getOnlineMemberCount: Not initialized or teamId is null");
            }
            return 0;
        }

        try {
            Object team = teamManager.getClass()
                    .getMethod("getTeam", UUID.class)
                    .invoke(teamManager, teamId);
            
            if (team == null) {
                if (debugMode) {
                    debug("getOnlineMemberCount: Team not found for " + teamId);
                }
                return 0;
            }

            // Try getOnlineMembers() first (if available)
            try {
                Object onlineMembers = team.getClass().getMethod("getOnlineMembers").invoke(team);
                if (onlineMembers instanceof java.util.Collection<?> collection) {
                    int count = collection.size();
                    if (debugMode) {
                        debug("getOnlineMemberCount: Team " + teamId + " has " + count + " online members (via getOnlineMembers)");
                    }
                    return count;
                }
            } catch (NoSuchMethodException e) {
                if (debugMode) {
                    debug("getOnlineMemberCount: getOnlineMembers() not available, using fallback");
                }
            }
            
            // Fallback: Team.getMembers() returns Collection<OfflinePlayer>
            Object members = team.getClass().getMethod("getMembers").invoke(team);
            
            if (members instanceof java.util.Collection<?> collection) {
                int count = (int) collection.stream()
                        .filter(member -> {
                            try {
                                if (member instanceof org.bukkit.OfflinePlayer offlinePlayer) {
                                    return offlinePlayer.isOnline();
                                }
                                // Try to get UUID and check if online
                                UUID memberId = (UUID) member.getClass().getMethod("getUniqueId").invoke(member);
                                return Bukkit.getPlayer(memberId) != null;
                            } catch (Exception ex) {
                                return false;
                            }
                        })
                        .count();
                if (debugMode) {
                    debug("getOnlineMemberCount: Team " + teamId + " has " + count + " online members (via getMembers filter)");
                }
                return count;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ReactorModule] Failed to get online member count for team " + teamId + ": " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
        }
        
        return 0;
    }

    /**
     * Gets all teams with at least one online member
     * Returns a collection of team UUIDs
     */
    public static java.util.Collection<UUID> getAllTeamsWithOnlineMembers() {
        java.util.Set<UUID> teams = new java.util.HashSet<>();
        
        if (!initialized || teamManager == null) {
            if (debugMode) {
                debug("getAllTeamsWithOnlineMembers: Not initialized");
            }
            return teams;
        }

        try {
            // Try to get all teams
            Object allTeams = teamManager.getClass().getMethod("getTeams").invoke(teamManager);
            
            if (allTeams instanceof java.util.Collection<?> collection) {
                for (Object team : collection) {
                    try {
                        UUID teamId = (UUID) team.getClass().getMethod("getId").invoke(team);
                        int onlineCount = getOnlineMemberCount(teamId);
                        if (onlineCount > 0) {
                            teams.add(teamId);
                            if (debugMode) {
                                debug("getAllTeamsWithOnlineMembers: Found team " + teamId + " with " + onlineCount + " online members");
                            }
                        }
                    } catch (Exception e) {
                        if (debugMode) {
                            debug("getAllTeamsWithOnlineMembers: Error processing team: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ReactorModule] Failed to get all teams: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
        }
        
        return teams;
    }

    /**
     * Adds currency to a team
     */
    public static boolean addCurrencyToTeam(UUID teamId, double amount) {
        if (!initialized || teamManager == null || teamId == null) {
            return false;
        }

        try {
            Object team = teamManager.getClass()
                    .getMethod("getTeam", UUID.class)
                    .invoke(teamManager, teamId);
            
            if (team == null) {
                return false;
            }

            // Try to add currency - method name may vary
            try {
                team.getClass().getMethod("addBalance", double.class).invoke(team, amount);
                return true;
            } catch (NoSuchMethodException e) {
                // Try alternative method names
                try {
                    team.getClass().getMethod("deposit", double.class).invoke(team, amount);
                    return true;
                } catch (NoSuchMethodException ex) {
                    // Try setBalance or other methods
                    try {
                        // Get current balance and add
                        double currentBalance = (Double) team.getClass().getMethod("getBalance").invoke(team);
                        team.getClass().getMethod("setBalance", double.class).invoke(team, currentBalance + amount);
                        return true;
                    } catch (Exception ex2) {
                        // Currency system might not be available
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
}
