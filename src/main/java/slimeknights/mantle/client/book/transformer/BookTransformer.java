package slimeknights.mantle.client.book.transformer;

import slimeknights.mantle.client.book.data.BookData;
import slimeknights.mantle.client.book.data.PageData;
import slimeknights.mantle.client.book.data.SectionData;
import slimeknights.mantle.client.book.data.content.ContentPadding.PaddingBookTransformer;
import slimeknights.mantle.client.book.data.content.ContentTableOfContents;
import slimeknights.mantle.client.book.data.element.TextData;

import java.util.List;

public abstract class BookTransformer {
  /** Adds a transformer which builds a visual index */
  public static BookTransformer indexTranformer() {
    return IndexTransformer.INSTANCE;
  }

  /** Adds a transformer which builds a table of contents */
  public static BookTransformer contentTableTransformer() {
    return ContentTableTransformer.INSTANCE;
  }

  /** Adds a transformer which builds a table of contents for a specific section name */
  @SuppressWarnings("unused") // API
  public static BookTransformer contentTableTransformerForSection(String sectionName) {
    return new ContentTableTransformer(sectionName);
  }

  /** Adds a transformer which removes padding pages if unneeded, should be added to the book last */
  public static BookTransformer paddingTransformer() {
    return PaddingBookTransformer.INSTANCE;
  }

  /**
   * Called when all the sections within the book are loaded.
   *
   * @param book The object to the book to be transformed
   */
  public abstract void transform(BookData book);

  /** Compatibility name for integrations using the former nested transformer. */
  @Deprecated
  protected static class ContentTableTransformer extends slimeknights.mantle.client.book.transformer.ContentTableTransformer {
    public static final ContentTableTransformer INSTANCE = new ContentTableTransformer();
    public ContentTableTransformer() { super(); }
    public ContentTableTransformer(String section) { super(section); }
  }
}
