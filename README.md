# HelpMates-Sponge-
Little Helper Robots

Scriptable helpers for your Server - Who needs pipes anyways :)

# This Plugin requires [MikesToolBox](https://github.com/DosMike/MikesToolBox-Sponge-)

## Crafting

Craft Robots with the following recipes:
```
 +    | + Comparator (4) 
+O+   | O Eye of Ender (1)
 +    |
     Robot Part (3)
```

```
 o    | o Skeleton Head (1)
v#v   | # Armor Stand (1)
 v    | v Robot Part (3)
     Helper Core (1)
```

```
+h+   | + Spiderweb (4)
c#l   | h Leather Helmet (1), c Leather Chestplate (1), l Leather Leggings (1), b Leather Boots (1)
+b+   | # Helper Core (1)
     HelpMate (1)
```
![Fancy Image](https://raw.githubusercontent.com/DosMike/HelpMates-Sponge-/master/hm-crafting.png "Fancy Crafting Image")

You can convert the type of a helpmate by renaming 1 Lapiz into the entity type (e.g. rename it to `minecraft:villager`)   
Then take the Lapiz and the HelpMate into both hands and use `/hm cast`   
The command requires the permission `helpmates.command.cast.base` and `helpmates.cast.entity.MODID.TYPE` (e.g. `helpmates.cast.entity.minecraft.villager`)

*Be warned: Some mob types can and will cause harm - thus it is not recommended to permit entities with wildcards!*

## HelpMates

You place HelpMates by holding them and right-clicking a block   
Once placed you can pick the HelpMate back up by sneaking and left-clicking   
Open the inventory of a HelpMate by right-clicking them
A regular left-click will print the HelpMates current task into chat (Hover the name to see fuel and status)

Helpmates consume about 16 Redstone per hour runtime, with a maximum fuel capacity of about 7h

To get a helpmate going write a script in a book and left-click the HelpMate with the book

## Scripting

The Programming Language for HelpMates is made of a non-conditional sequence of commands.   
Here are the available commands:

| Command | Parameter | Description |
| --- | --- | ---|
| `charge` | max | Takes `max` amount of redstone from his Inventory and consumes it for fuel
| `name is at location` | | Saves a `location` with the specified `name` |
| `goto` | name or location | Let this HelpMate navigate to the location |
| `use` | name or location | Open a container/block at this location (will goto if necessary), can also open doors and push buttons |
| `put` | item name or `something` or `anything` or `everything` | Put stuff in the used block/container |
| `fuel` | item name or `something` or `anything` or `everything` | Like put, but prefer fuel slots |
| `take` | item name or `something` or `anything` or `everything` | Take stuff out of the used block/container |
| `wait` | seconds | Wait the specified amount of seconds |
| `try to` | another command | Don't wait until the command is done, just give it a quick try and move on |
| `repeat` | - | Start the script again |
| `tpto` | name or location | Teleport this HelpMate. Requires the owner to have permission `helpmates.script.teleport` |

In theory `use` would request permission, but at this point no permission plugin is checking fake interactions this way.

`anything` means 'once, the first available, if possible'   
`something` means 'the first available, required'   
`everything` means 'until no more items are accepted'

Books containing scripts DO NOT have to be signed, they can still be editable

## Permissions

| Permission | Description |
| --- | --- |
| `helpmates.create.base` | Allow spawning helpmates with right-click (not related to crafting) |
| `helpmate.skript.teleport` | Allow a players HelpMate to execute the script command `TpTo` |
| `helpmates.cast.entity.MODID.TYPE` | Allow a HelpMate to be casted into the type MODID:TYPE |
| `helpmates.command.give.base` | Allows to cheat HelpMates with `/hm create` |
| `helpmates.command.delete.base` | Allows `/hm delete` to delete a players HelpMate |
| `helpmates.command.cast.base` | Allows casting HelpMates into other types with `/hm cast` |

Options:
* `helpmates.option.create.limit` - Maximum amount a player may have at a time

## Commands

| Command | Alias | Description |
| --- | --- | --- |
| /hm create | create, give, op, opmate | Create a HelpMate.<br>-t EntityType will overwrite the entity type,<br>-f Fuel will set the initial fuel (max 100000) |
| /hm delete <Player> | delete | Remove all HelpMates spawned by this player |
| /hm cast | cast, convert, change, type | Allows to change type with named lapis |
