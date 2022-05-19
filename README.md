# Følg

> Stupidly simplistic self-hosted blogging platform designed for offline composition

The goal is to accommodate offline composition and publishing posts from any computer (e.g. smartphone using a file explorer app), which you'll use to create a directory with compressed images and a markdown file, then transfer it to your server once you're online. This program is only a static site generator - it's up to you to create the directories and files according to the spec, and provide a method for uploading to your server.

## Client usage

A markdown file needs to have a title, date and body. See [markdown-clj](https://github.com/yogthos/markdown-clj#supported-syntax) for supported syntax.

```
Title: Hello There!
Date: 2022-05-01

This is the first post.
```

Place markdown and image files into `resources/public` under a directory with a URL-friendly name. Each directory represents a single blog post with one markdown file optionally followed by one or more images. Blog posts are sorted in reverse, so that the chronologically most recent post is at the top. Images within a blog post are appended after the markdown text, and are sorted with increasing filenames.

```
▾ resources/public/
  ▾ hello/
      post.md
  ▾ bye/
      post.md
      P1050287.JPG
      P1050291.JPG
```

Beyond ordering of images, filenames have no relevance.

## Server usage

Generate static files for a blog to the specified directory (will delete any contents).

```
clojure -X folg/build :title '"My Blog"' :out '"/var/www/blog"'
```

Build initially, then rebuild on changes to resources.

```
clojure -X folg/watch :title '"My Blog"' :out '"/var/www/blog"'
```

By default, you'll get a *stream of thought* layout, which is suitable for many short posts with little text and few images. If you write fewer longer posts, you would probably prefer having a *table of contents* with each post being their own page. You can enable this with the `:toc true` argument.

## Customisation

Just edit `src/folg.clj`. Thanks to the excellent libraries it uses, it's very little code.
