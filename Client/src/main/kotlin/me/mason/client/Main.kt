package me.mason.client

suspend fun main(args: Array<String>) =
    if (args.isEmpty()) client()
    else if (args.size == 2) editor(args[0], Integer.parseInt(args[1]))
    else error("Invalid startup arguments")