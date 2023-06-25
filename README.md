# Capsulego

A clojure/babashka script for converting a gemlog into a gopherhole.

Requires - [babashka](https://github.com/babashka/babashka)

``` shell
$ bash < <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install)
```

## Usage

Copy the main script `capsulego` somewhere on your `PATH` and make executable. Optionally, copy the `config.edn` file to the same directory, if you want to change the configs (or you can also change them directly in the script)

There are two ways you can use the script:

1. Converting a single file - `file`
2. Converting an entire capsule - `capsule`

In the case of converting a single file, the options are:

- `-f` file you wish to convert (required)
- `-o` filename for output  (defaults to `filename_gopher.txt`)
- `-d` domain name (defaults to `localhost`)

``` shell
capsulego file -f "myfile.gmi" -o "myfile.txt" -d "example.com"
```


In the case of converting a capsule, the options are similar:
- `-c` directory (capsule) you wish to convert
- `-o` name of output directory (defaults to `capsulename_gopher`)
- `-d` domain name (defaults to `localhost`)

``` shell
capsulego capsule -c "/path/to/gemine/capsule" -o "/var/gopher" -d "example.com"
```

## Options

Some options can be set in the `config.edn` file, providing it is in the same directory as the script. Available options are:

- Line length - number
  - The point to wrap lines at, e.g., 67

- Headers - true/false
  - Whether to include an auto-generated header in gopher text files
  - Header format is:

``` text

======================================================
Title: <Post Title>
Date: <Post Date>
Word Count: <word count of post>
======================================================

```

- File-extension-preference - `""` or `".txt"` 
  - Whether to output text files with no extensions (example) or with explicit .txt extension (example.txt)


- Overwrite - true/false
  - Whether to overwrite output directory if already exists

## Formatting

I've adopted the formatting choices for the plaintext from [Ruarí Ødegaard's gemtext to gopher bash script](https://ruario.flounder.online/gemlog/2022-01-04_Formatting_Gemtext_for_Gopher.gmi) 

Gemini linetypes:

``` text
# Heading 1
## Heading 2
### Heading 3
> Quote 
```preformatted text
=> link.gmi A link
* List

A regular line.
```

Gopher  equivalents:

``` text
Heading1
========

Heading 2
---------

-Heading 3-

> Quote Line 1
> Quote Line 2

```preformatted (nowrap)

~ Link label:
  gopher://example.org

* List ...
  With wrapped text indented
  
A regular line (will get wrapped at 68 characters)
```

