package org.linkwave.chatservice.message;

/**
 * Content of a file message together with the metadata needed to serve it safely.
 *
 * @param content     file bytes
 * @param contentType MIME type detected at upload time
 * @param filename    sanitized original file name
 */
public record AttachedFile(byte[] content, String contentType, String filename) {
}