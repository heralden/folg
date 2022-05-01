# Følg

> Stupidly simplistic self-hosted short-blogging platform.

Place markdown and image files into `resources/public` under a directory with a name to be ordered (usually YYYY-MM-DD). Each directory represents a single blog post with one markdown file optionally followed by one or more images. Blog posts are concatenated into a single *index.html* and sorted in reverse, so that the chronologically most recent post is at the top. Images within a blog post are sorted with the oldest first.

```
▾ resources/public/
  ▾ 2022-04-30/
      post.md
  ▾ 2022-05-01/
      post.md
      P1050287.JPG
      P1050291.JPG
```

Beyond ordering, directory and file names have no relevance.

The intention is to accommodate publishing posts from a smartphone using a file explorer app, which you'll use to create a directory with compressed images (to save mobile data) and a markdown file, then transfer it to your server.

## Usage

Generate static files for a blog to the specified directory (will delete any contents).

```
clj -X folg/build :out /var/www/blog
```

Build initially, then rebuild on changes to resources.

```
clj -X folg/watch :out /var/www/blog
```

## Customisation

Just edit `src/folg.clj`. Thanks to the excellent libraries it uses, it's very little code.
