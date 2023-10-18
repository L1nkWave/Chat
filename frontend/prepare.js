const fs = require('fs');

if (fs.existsSync('.git')) {
  // eslint-disable-next-line import/no-extraneous-dependencies
  require('husky').install();
}
