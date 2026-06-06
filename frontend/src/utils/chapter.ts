type ChapterLike = {
  chapterNo: number;
};

export function getNextChapterNo(chapters: ChapterLike[]) {
  if (chapters.length === 0) {
    return 1;
  }

  return Math.max(...chapters.map((chapter) => chapter.chapterNo)) + 1;
}

export function canStartAdaptation(chapterCount: number) {
  return chapterCount >= 3;
}
