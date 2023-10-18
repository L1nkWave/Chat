const ERROR = 2;

const OFF = 0;

const COUNT = 1;

const REACT_RULES = {
  'react/no-children-prop': ERROR,
  'react/no-deprecated': ERROR,
  'react/no-multi-comp': ERROR,
  'react/no-string-refs': ERROR,
  'react/no-unescaped-entities': ERROR,
  'react/no-unstable-nested-components': ERROR,
  'react/self-closing-comp': ERROR,
  'react/jsx-boolean-value': [ERROR, 'always'],
  'react/jsx-fragments': ERROR,
  'react/jsx-handler-names': [
    ERROR,
    {
      checkInlineFunction: true,
      checkLocalVariables: true,
    },
  ],
  'react/jsx-key': ERROR,
  'react/jsx-no-leaked-render': ERROR,
  'react/jsx-no-useless-fragment': ERROR,
  'react/jsx-sort-props': ERROR,
};

const ESLINT_RULES = {
  'no-console': ERROR,
  'object-shorthand': ERROR,
  'no-magic-numbers': [ERROR, { ignoreArrayIndexes: true, ignore: [1, 0, -1] }],
};

const TYPESCRIPT_RULES = {
  '@typescript-eslint/no-unused-vars': [
    ERROR,
    {
      argsIgnorePattern: '^_',
      varsIgnorePattern: '^_',
    },
  ],
  '@typescript-eslint/no-var-requires': OFF,
  '@typescript-eslint/no-explicit-any': [ERROR],
};

const IMPORT_RULES = {
  'import/export': OFF,
  'import/no-default-export': ERROR,
  'import/newline-after-import': [ERROR, { count: COUNT }],
  'import/order': [
    ERROR,
    {
      groups: [
        'builtin',
        'external',
        'parent',
        'sibling',
        'internal',
        'index',
        'object',
      ],
      pathGroups: [
        {
          pattern: '{react,recoil,recoil-nexus,}',
          group: 'external',
          position: 'before',
        },
        {
          pattern: '{components/**,modules,components/**}',
          group: 'parent',
          position: 'before',
        },
        {
          pattern: '{screens/**,modules,screens/**}',
          group: 'parent',
          position: 'before',
        },
        {
          pattern: '{assets/**,constants/**}',
          group: 'parent',
          position: 'before',
        },
        {
          pattern: '{helpers/**,services/**,navigators/**,store/**,}',
          group: 'parent',
          position: 'before',
        },
        {
          pattern: '{hooks/**,modules,hooks/**}',
          group: 'parent',
          position: 'before',
        },
        {
          pattern:
            '{**/*.types,**/types,./*.types,./types,**/*.settings,**/settings,./*.settings,./settings,**/*.styles,**/styles,./*.styles,./styles}',
          group: 'index',
          position: 'before',
        },
      ],
      pathGroupsExcludedImportTypes: ['react', 'builtin'],
      'newlines-between': 'always',
      alphabetize: {
        order: 'asc',
        caseInsensitive: false,
      },
    },
  ],
};

module.exports = {
  env: {
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:prettier/recommended',
    'plugin:import/recommended',
    'plugin:react-hooks/recommended',
    'plugin:import/typescript',
  ],
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint', 'react', 'import'],
  settings: {
    react: {
      version: 'detect',
    },
    'import/ignore': ['react'],
    'import/resolver': {
      node: {
        paths: ['src'],
        extensions: ['.js', '.ts', '.tsx', '.d.ts'],
      },
    },
  },
  rules: {
    ...REACT_RULES,
    ...ESLINT_RULES,
    ...TYPESCRIPT_RULES,
    ...IMPORT_RULES,
  },
};
