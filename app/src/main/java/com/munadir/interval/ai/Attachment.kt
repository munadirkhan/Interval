package com.munadir.interval.ai

/** What kind of thing the user attached, which decides how much of it we can actually read. */
enum class AttachmentKind { TEXT, DOCUMENT, PHOTO }

/**
 * A file or photo the user attached to a generation request.
 *
 * Plain-text files are read in full and become the source material. PDFs and photos are
 * accepted and shown, but their contents are not parsed -- on-device PDF text extraction and
 * OCR are each a dependency and a failure mode this app deliberately does not carry. For those,
 * [extractedText] is null and the file name is passed as a topic hint instead.
 *
 * [note] is the honest one-line explanation shown under the chip, so nobody is misled into
 * thinking a scanned page was read.
 */
data class Attachment(
    val name: String,
    val kind: AttachmentKind,
    val extractedText: String? = null
) {
    val isReadable: Boolean get() = !extractedText.isNullOrBlank()

    val note: String
        get() = when {
            isReadable -> "Read ${extractedText!!.length} characters"
            kind == AttachmentKind.PHOTO -> "Photo attached — used as a topic hint"
            else -> "Attached — used as a topic hint"
        }
}
