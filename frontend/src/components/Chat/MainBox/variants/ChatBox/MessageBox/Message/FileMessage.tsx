import "./fileMessage.styles.css";

import Image from "next/image";
import { useCallback, useEffect, useState } from "react";

import { getFile } from "@/api/http/chat/chat";
import { MessageParams } from "@/api/http/contacts/contacts.types";

export function FileMessage({ message }: Readonly<{ message: MessageParams }>) {
  const [fileUrl, setFileUrl] = useState<string>("");
  const [isImage, setIsImage] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);

  function blobToBase64(blob: Blob): Promise<string> {
    return new Promise(resolve => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result as string);
      reader.readAsDataURL(blob);
    });
  }

  const fetchFile = useCallback(async () => {
    setLoading(true);
    try {
      const file = await getFile(message.id);
      const blob = new Blob([file], { type: message.contentType });

      if (message.contentType?.startsWith("image/")) {
        const base64Image = await blobToBase64(blob);
        setFileUrl(base64Image);
        setIsImage(true);
      } else {
        const objectUrl = URL.createObjectURL(blob);
        setFileUrl(objectUrl);
        setIsImage(false);
      }
    } catch (error) {
      console.error("Error fetching file:", error);
    } finally {
      setLoading(false);
    }
  }, [message.contentType, message.id]);

  useEffect(() => {
    fetchFile();
    return () => {
      if (fileUrl && !isImage) {
        URL.revokeObjectURL(fileUrl);
      }
    };
  }, []);

  return (
    <div className="flex items-center justify-center">
      {loading ? (
        <div className="loader" />
      ) : isImage ? (
        <Image quality={10} src={fileUrl} alt="Image" className="rounded-2xl h-auto w-auto max-w-[700px]" width={256} height={128} />
      ) : (
        <a href={fileUrl} download={`file_${message.id}`} className="text-blue-500 underline">
          Download file
        </a>
      )}
    </div>
  );
}
