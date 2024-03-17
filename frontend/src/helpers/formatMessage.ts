export const formatMessage = (input: string | undefined): string | undefined => {
  if (!input) {
    return input;
  }
  const capitalizedString = input.charAt(0).toUpperCase() + input.slice(1);
  return /[.!]$/.test(capitalizedString) ? capitalizedString : `${capitalizedString}.`;
};
