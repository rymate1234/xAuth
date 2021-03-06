package com.cypherx.xauth.commands;

import java.lang.reflect.Field;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.cypherx.xauth.Account;
import com.cypherx.xauth.TeleLocation;
import com.cypherx.xauth.Util;
import com.cypherx.xauth.xAuth;
import com.cypherx.xauth.xAuthLog;
import com.cypherx.xauth.xAuthMessages;
import com.cypherx.xauth.xAuthPermissions;
import com.cypherx.xauth.xAuthPlayer;
import com.cypherx.xauth.xAuthSettings;
import com.cypherx.xauth.datamanager.DataManager;

public class xAuthCommand implements CommandExecutor {
	private final xAuth plugin;
	private DataManager dataManager;

	public xAuthCommand(xAuth plugin) {
	    this.plugin = plugin;
	    this.dataManager = plugin.getDataManager();
	}

	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length < 1)
			return false;

		String subCommand = args[0];
		if (subCommand.equals("register"))
			return registerCommand(sender, args);
		else if (subCommand.equals("changepw") || subCommand.equals("cpw") || subCommand.equals("changepassword") || subCommand.equals("changepass"))
			return changePasswordCommand(sender, args);
		else if (subCommand.equals("logout"))
			return logoutCommand(sender, args);
		else if (subCommand.equals("unregister"))
			return unregisterCommand(sender, args);
		else if (subCommand.equals("location") || subCommand.equals("loc"))
			return locationCommand(sender, args);
		else if (subCommand.equals("config") || subCommand.equals("conf"))
			return configCommand(sender, args);
		else if (subCommand.equals("reload"))
			return reloadCommand(sender);
		else if (subCommand.equals("version"))
			return versionCommand(sender);
		else {
			if (sender instanceof Player)
				xAuthMessages.send("admnUnknown", (Player)sender);
			else if (sender instanceof ConsoleCommandSender)
				xAuthLog.info("Unknown subcommand, try \"xauth\" for more information");
		}

		return true;
	}

	private boolean versionCommand(CommandSender sender) {
		if (sender instanceof Player)
			((Player)sender).sendMessage("[" + xAuth.desc.getName() + "] This server is running version " + xAuth.desc.getVersion());
		else if (sender instanceof ConsoleCommandSender)
			xAuthLog.info("This server is running version " + xAuth.desc.getVersion());

		return true;
	}

	private boolean registerCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;

			if (!xAuthPermissions.has(player, "xauth.admin.register")) {
				xAuthMessages.send("admnPermission", player);
				return true;
			} else if (args.length < 3) {
				xAuthMessages.send("admnRegUsage", player);
				return true;
			}

			String targetName = args[1];
			String password = args[2];
			String email = (args.length > 3 ? args[3] : null);
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (xPlayer.isRegistered()) {
				xAuthMessages.send("admnRegRegistered", player, xPlayer.getPlayer());
				return true;
			}

			Account account = new Account(targetName, Util.encrypt(password), email);
			xPlayer.setAccount(account);
			dataManager.saveAccount(account);

			xAuthMessages.send("admnRegSuccess", player, xPlayer.getPlayer());
			xAuthLog.info(player.getName() + " has registered an account for " + targetName);
		} else if (sender instanceof ConsoleCommandSender) {
			if (args.length < 3) {
				xAuthLog.info("Correct Usage: xauth register <player> <password> [email]");
				return true;
			}

			String targetName = args[1];
			String password = args[2];
			String email = (args.length > 3 ? args[3] : null);
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (xPlayer.isRegistered()) {
				xAuthLog.info(targetName + " is already registered!");
				return true;
			}

			Account account = new Account(targetName, Util.encrypt(password), email);
			xPlayer.setAccount(account);
			dataManager.saveAccount(account);

			xAuthLog.info("Account successfully created for: " + targetName);
		}

		return true;
	}

	private boolean changePasswordCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;

			if (!xAuthPermissions.has(player, "xauth.admin.changepw")) {
				xAuthMessages.send("admnPermission", player);
				return true;
			} else if (args.length < 3) {
				xAuthMessages.send("admnCpwUsage", player);
				return true;
			}

			String targetName = args[1];
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (!xPlayer.isRegistered()) {
				xAuthMessages.send("admnCpwRegistered", player, xPlayer.getPlayer());
				return true;
			}

			Account targetAcc = xPlayer.getAccount();
			String newPassword = args[2];

			plugin.changePassword(targetAcc, newPassword);
			xAuthMessages.send("admnCpwSuccess", player, xPlayer.getPlayer());
			xAuthLog.info(player.getName() + " changed " + targetName + "'s password");
		} else if (sender instanceof ConsoleCommandSender) {
			if (args.length < 3) {
				xAuthLog.info("Correct Usage: xauth changepw <player> <new password>");
				return true;
			}

			String targetName = args[1];
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (!xPlayer.isRegistered()) {
				xAuthLog.info("This player is not registered!");
				return true;
			}

			Account targetAcc = xPlayer.getAccount();
			String newPassword = args[2];

			plugin.changePassword(targetAcc, newPassword);
			xAuthLog.info(targetName + "'s password has been changed");
		}

		return true;
	}

	private boolean logoutCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;

			if (!xAuthPermissions.has(player, "xauth.admin.logout")) {
				xAuthMessages.send("admnPermission", player);
				return true;
			} else if (args.length < 2) {
				xAuthMessages.send("admnLogoutUsage", player);
				return true;
			}

			String targetName = args[1];
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (!xPlayer.hasSession()) {
				xAuthMessages.send("admnLogoutLogged", player, xPlayer.getPlayer());
				return true;
			}

			plugin.createGuest(xPlayer);
			Player target = xPlayer.getPlayer();
			xAuthMessages.send("admnLogoutSuccess", player, target);
			if (target != null)
				xAuthMessages.send("logoutSuccess", target);
			xAuthLog.info(targetName + " was logged out by " + player.getName());
		} else if (sender instanceof ConsoleCommandSender) {
			if (args.length < 2) {
				xAuthLog.info("Correct Usage: xauth logout <player>");
				return true;
			}

			String targetName = args[1];
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (!xPlayer.hasSession()) {
				xAuthLog.info(targetName + " is not logged in!");
				return true;
			}

			plugin.createGuest(xPlayer);
			xAuthLog.info(targetName + " has been logged out");
			Player target = xPlayer.getPlayer();
			if (target != null)
				xAuthMessages.send("logoutSuccess", target);
		}

		return true;
	}

	private boolean unregisterCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;

			if (!xAuthPermissions.has(player, "xauth.admin.unregister")) {
				xAuthMessages.send("admnPermission", player);
				return true;
			} else if (args.length < 2) {
				xAuthMessages.send("admnUnregUsage", player);
				return true;
			}

			String targetName = args[1];
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (!xPlayer.isRegistered()) {
				xAuthMessages.send("admnUnregRegistered", player, xPlayer.getPlayer());
				return true;
			}

			dataManager.deleteAccount(xPlayer);

			Player target = xPlayer.getPlayer();
			if (target != null) {
				if (xPlayer.mustRegister())
					plugin.createGuest(xPlayer);
				xAuthMessages.send("admnUnregSuccessTgt", target);
			}

			xAuthMessages.send("admnUnregSuccessPlyr", player, target);
			xAuthLog.info(targetName + " has been unregistered by " + player.getName());
		} else if (sender instanceof ConsoleCommandSender) {
			if (args.length < 2) {
				xAuthLog.info("Correct Usage: xauth unregister <player>");
				return true;
			}

			String targetName = args[1];
			xAuthPlayer xPlayer = dataManager.getPlayer(targetName);

			if (!xPlayer.isRegistered()) {
				xAuthLog.info(targetName + " is not registered!");
				return true;
			}

			dataManager.deleteAccount(xPlayer);
			
			Player target = xPlayer.getPlayer();
			if (target != null) {
				if (xPlayer.mustRegister())
					plugin.createGuest(xPlayer);
				target.sendMessage("You have been unregistered and logged out!");
			}

			xAuthLog.info(targetName + " has been unregistered!");
		}

		return true;
	}

	private boolean locationCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;

			if (!xAuthPermissions.has(player, "xauth.admin.location")) {
				xAuthMessages.send("admnPermission", player);
				return true;
			} else if (args.length < 2 || !(args[1].equals("set") || args[1].equals("remove"))) {
				xAuthMessages.send("admnLocUsage", player);
				return true;
			}

			String action = args[1];

			if (action.equals("set")) {
				dataManager.setTeleLocation(new TeleLocation(player.getLocation()));
				xAuthMessages.send("admnLocSetSuccess", player);
			} else {
				TeleLocation tLoc = dataManager.getTeleLocation(player.getWorld().getName());
				if (tLoc == null) {
					xAuthMessages.send("admnLocRmvNo", player);
					return true;
				}

				dataManager.removeTeleLocation(tLoc);
				xAuthMessages.send("admnLocRmvSuccess", player);
			}
		}

		return true;
	}

	private boolean configCommand(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			Player player = (Player)sender;

			if (!xAuthPermissions.has(player, "xauth.admin.config")) {
				xAuthMessages.send("admnPermission", player);
				return true;
			} else if (args.length < 2) {
				xAuthMessages.send("admnConfUsage", player);
				return true;
			}

			String setting = args[1];
			Object value = (args.length > 2 ? args[2] : null);
			Field field;

			try {
				field = xAuthSettings.class.getField(setting);
			} catch (NoSuchFieldException e) {
				xAuthMessages.send("admnConfNo", player);
				return true;
			}

			String type = null;
			if (field.getType().equals(String.class))
				type = "String";
			else if (field.getType().equals(Integer.TYPE))
				type = "Integer";
			else if (field.getType().equals(Boolean.TYPE))
				type = "Boolean";

			if (value == null) { // view setting info
				//player.sendMessage("Setting: " + field.getName());
				//player.sendMessage("Type:    " + type);
				try {
					xAuthMessages.sendConfigDesc(player, field.getName(), type, field.get(xAuthSettings.class));
					//player.sendMessage("Value:   " + field.get(xAuthSettings.class));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}

				return true;
			} else { // change setting
				try {
					if (type.equals("String"))
						field.set(xAuthSettings.class, value.toString());
					else if (type.equals("Integer"))
						field.set(xAuthSettings.class, Integer.parseInt(value.toString()));
					else if (type.equals("Boolean"))
						field.set(xAuthSettings.class, Boolean.parseBoolean(value.toString()));
					else
						throw new IllegalArgumentException();
				} catch (IllegalArgumentException e) {
					xAuthMessages.send("admnConfInvalid", player);
					return true;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return true;
				}

				xAuthSettings.changed = true;
				xAuthMessages.send("admnConfSuccess", player);
				xAuthLog.info(player.getName() + " has changed setting " + setting + " to " + value);
			}
		} else if (sender instanceof ConsoleCommandSender) {
			if (args.length < 2) {
				xAuthLog.info("Correct Usage: xauth config <setting> [new value]");
				return true;
			}

			String setting = args[1];
			Object value = (args.length > 2 ? args[2] : null);
			Field field = null;

			try {
				field = xAuthSettings.class.getField(setting);
			} catch (NoSuchFieldException e) {
				xAuthLog.info("No such setting!");
				return true;
			}

			String type = null;
			if (field.getType().equals(String.class))
				type = "String";
			else if (field.getType().equals(Integer.TYPE))
				type = "Integer";
			else if (field.getType().equals(Boolean.TYPE))
				type = "Boolean";

			if (value == null) { // view setting info
				xAuthLog.info("Setting: " + field.getName());
				xAuthLog.info("Type:    " + type);
				try {
					xAuthLog.info("Value:   " + field.get(xAuthSettings.class));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return true;
				}
			} else { // change setting
				try {
					if (type.equals("String"))
						field.set(xAuthSettings.class, value.toString());
					else if (type.equals("Integer"))
						field.set(xAuthSettings.class, Integer.parseInt(value.toString()));
					else if (type.equals("Boolean"))
						field.set(xAuthSettings.class, Boolean.parseBoolean(value.toString()));
					else
						throw new IllegalArgumentException();
				} catch (IllegalArgumentException e) {
					xAuthLog.info("Invalid type");
					return true;
				} catch (IllegalAccessException e) {
					e.printStackTrace();
					return true;
				}

				xAuthSettings.changed = true;
				xAuthLog.info("Setting changed!");
			}
		}

		return true;
	}

	private boolean reloadCommand(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player)sender;

			if (xAuthPermissions.has(player, "xauth.admin.reload")) {
				plugin.reload();
				xAuthMessages.send("admnReloadSuccess", player);
				xAuthLog.info("Reloaded by " + player.getName());
			} else
				xAuthMessages.send("admnPermission", player);
		} else if (sender instanceof ConsoleCommandSender) {
			plugin.reload();
			xAuthLog.info("Reload complete!");
		}

		return true;
	}
}