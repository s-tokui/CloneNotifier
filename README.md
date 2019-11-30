# Clone Notifier

## Demonstration Video

[![](https://img.youtube.com/vi/KCzsPjPJaPw/0.jpg)](https://www.youtube.com/watch?v=KCzsPjPJaPw)

## About this system
- Code clone is a code fragment that has identical or similar code fragments to it in the source code. It is pointed out as one of the main problems in software maintenance.
- When a maintainer fixes a defect, he/she has to find the code clones corresponding to the code fragment.
- Clone Notifier is a system that notifies creations and changes of code clones to software maintainers.
- At first, Clone Notifier identifies creations and changes of code clones. And then it groups them into four categories (e.g., new, deleted, changed) and also assigns labels  (e.g., consistent, inconsistent) to them. Finally, it notifies creations and changes of code clones together with the corresponding categories and labels. 

## Clone Detectors Used by Clone Notifier
- [SourcererCC](https://github.com/Mondego/SourcererCC)
- [CCFinderX](http://www.ccfinder.net/ccfinderx-j.html)
- [CCVolti](https://github.com/k-yokoi/CCVolti)
