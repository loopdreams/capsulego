# Capsulego

Some clojure/babashka scripts for converting a gemlog into a gopherhole.

Usage:

`capsulego capsule gemini-capsule-directory -d domain-name -o gopher-directory -i index-files`

`capsulego single gemtext-file`

`capsulego index index-file`


- 'index' files are files that will be converted to gophermaps. Files named `index.gmi` are automatically converted to gophermaps.


## Formatting

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

Gopher `.txt` equivalents:

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

I've adopted the formatting choices for the plaintext from [Ruarí Ødegaard's gemtext to gopher bash script](https://ruario.flounder.online/gemlog/2022-01-04_Formatting_Gemtext_for_Gopher.gmi) 
