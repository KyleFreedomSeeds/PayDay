PayDay is a Sponge plugin that pays players at set intervals.

## Usage

A configuration file (`payday.conf`) is located in the Sponge configuration directory, and allows you to modify how the plugin functions.

```
timeamount="1"
timeunit=Hours
payamounts={
    "players"= {
        "permission"= "*",
        "amount"= 50.0
    }
}
```

Valid time units are:

- `Nanoseconds`
- `Microseconds`
- `Milliseconds`
- `Seconds`
- `Minutes`
- `Hours`
- `Days`

Pay amounts are assigned using the following format:

```
payamounts={
    "my group"= {
        "permission"= "*",
        "amount"= 50.0
    },
    "my group 2"= {
        "permission"= "requires.this.permission",
        "amount"= 19.99
    }
}
```

If the permission is set to `*`, it will pay all players.

---

## Credit

PayDay was originally created by HassanS6000 of [NEGAFINITY](http://negafinity.com).
