import Image from "next/image";

export function ChatItem() {
  return (
    <button
      type="button"
      className="flex flex-row outline-none bg-dark-300 p-4 rounded-lg justify-between items-center hover:bg-dark-200"
    >
      <div className="flex flex-row">
        <div className="w-14 h-14">
          <Image
            className="object-cover rounded-full"
            src="/images/logo.svg"
            alt="User avatar"
            width={64}
            height={64}
          />
        </div>
        <div className="flex flex-col ml-4 justify-center items-start">
          <span className="text-lg">User Name</span>
          <p className="text-gray-300">Message can be... </p>
        </div>
      </div>
      <div>
        <p className="text-gray-300 text-sm">16:50</p>

        <div className="flex justify-center items-center bg-blue-300 rounded-lg mt-1">
          +3
        </div>
      </div>
    </button>
  );
}
