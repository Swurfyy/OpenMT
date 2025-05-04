package nl.openminetopia.modules.lock.utils;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.experimental.UtilityClass;
import nl.openminetopia.OpenMinetopia;
import nl.openminetopia.utils.WorldGuardUtils;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@UtilityClass
public class LockUtil {

    public void setLocked(Block block, UUID ownerUuid) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        data.set(new NamespacedKey(OpenMinetopia.getInstance(), "lock.owner"), DataType.UUID, ownerUuid);
    }

    public void removeLock(Block block) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        data.remove(new NamespacedKey(OpenMinetopia.getInstance(), "lock.owner"));
        data.remove(new NamespacedKey(OpenMinetopia.getInstance(), "lock.members"));
        data.remove(new NamespacedKey(OpenMinetopia.getInstance(), "lock.groups"));
    }

    public void addLockMember(Block block, UUID memberUuid) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "lock.members");

        String[] members = data.get(key, DataType.STRING_ARRAY);
        List<String> updatedMembers = members != null ? new ArrayList<>(List.of(members)) : new ArrayList<>();

        if (!updatedMembers.contains(memberUuid.toString())) {
            updatedMembers.add(memberUuid.toString());
        }

        data.set(key, DataType.STRING_ARRAY, updatedMembers.toArray(new String[0]));
    }

    public void removeLockMember(Block block, UUID memberUuid) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "lock.members");

        String[] members = data.get(key, DataType.STRING_ARRAY);
        if (members == null) return;

        List<String> updatedMembers = new ArrayList<>(List.of(members));
        updatedMembers.remove(memberUuid.toString());

        data.set(key, DataType.STRING_ARRAY, updatedMembers.toArray(new String[0]));
    }

    public List<UUID> getLockMembers(Block block) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "lock.members");

        String[] members = data.get(key, DataType.STRING_ARRAY);
        List<UUID> uuids = new ArrayList<>();

        if (members == null) return uuids;
        for (String member : members) {
            uuids.add(UUID.fromString(member));
        }

        return uuids;
    }

    public void addLockGroup(Block block, String group) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "lock.groups");

        String[] groups = data.get(key, DataType.STRING_ARRAY);
        List<String> updatedGroups = groups != null ? new ArrayList<>(List.of(groups)) : new ArrayList<>();

        if (!updatedGroups.contains(group)) {
            updatedGroups.add(group);
        }

        data.set(key, DataType.STRING_ARRAY, updatedGroups.toArray(new String[0]));
    }

    public void removeLockGroup(Block block, String group) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "lock.groups");

        String[] groups = data.get(key, DataType.STRING_ARRAY);
        if (groups == null) return;

        List<String> updatedGroups = new ArrayList<>(List.of(groups));
        updatedGroups.remove(group);

        data.set(key, DataType.STRING_ARRAY, updatedGroups.toArray(new String[0]));
    }

    public List<String> getLockGroups(Block block) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        NamespacedKey key = new NamespacedKey(OpenMinetopia.getInstance(), "lock.groups");

        String[] groups = data.get(key, DataType.STRING_ARRAY);
        List<String> groupList = new ArrayList<>();

        if (groups == null) return groupList;
        Collections.addAll(groupList, groups);

        return groupList;
    }

    public UUID getLockOwner(Block block) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        return data.get(new NamespacedKey(OpenMinetopia.getInstance(), "lock.owner"), DataType.UUID);
    }

    public boolean isLocked(Block block) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        return data.has(new NamespacedKey(OpenMinetopia.getInstance(), "lock.owner"), DataType.UUID);
    }

    public boolean canOpen(Block block, Player player) {
        PersistentDataContainer data = new CustomBlockData(block, OpenMinetopia.getInstance());
        if (!isLocked(block)) return true;

        if (player.hasPermission("openminetopia.lock.bypass")) return true;

        UUID ownerUuid = data.get(new NamespacedKey(OpenMinetopia.getInstance(), "lock.owner"), DataType.UUID);
        if (ownerUuid == null) return false;

        if (ownerUuid.equals(player.getUniqueId())) return true;

        List<UUID> members = getLockMembers(block);
        if (members.contains(player.getUniqueId())) return true;
        List<String> groups = getLockGroups(block);
        for (String group : groups) {
            if (player.hasPermission("group." + group)) return true;
        }

        return false;
    }

    public boolean isLockable(Block block) {
        if (block.getBlockData() instanceof Door) return true;
        if (block.getBlockData() instanceof TrapDoor) return true;
        if (block.getBlockData() instanceof Gate) return true;
        if (block.getBlockData() instanceof Chest) return true;
        if (block.getBlockData() instanceof Sign) return true;
        if (block.getBlockData() instanceof Furnace) return true;
        return block.getBlockData() instanceof Barrel;
    }
}
