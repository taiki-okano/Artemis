const webpack = require('webpack');
const { merge } = require('webpack-merge');
const path = require('path');
const MergeJsonWebpackPlugin = require('merge-jsons-webpack-plugin');
const BrowserSyncPlugin = require('browser-sync-webpack-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const WebpackNotifierPlugin = require('webpack-notifier');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');

const environment = require('./environment');

const tls = process.env.TLS;

interface EnvironmentOptions {
    // note: for some reason we have to use single and double quotes together here '"..."' to avoid warnings during webpack build
    env: 'production' | 'development';
}

module.exports = (config: any, options: EnvironmentOptions, targetOptions: any) => {
    config.cache = {
        // 1. Set cache type to filesystem
        type: 'filesystem',
        cacheDirectory: path.resolve(__dirname, '../build/webpack'),
        buildDependencies: {
            // 2. Add your config as buildDependency to get cache invalidation on config change
            config: [
                __filename,
                path.resolve(__dirname, 'webpack.custom.ts'),
                path.resolve(__dirname, '../angular.json'),
                path.resolve(__dirname, '../tsconfig.app.json'),
                path.resolve(__dirname, '../tsconfig.json'),
            ],
        },
    };

    if (targetOptions.target === 'serve' || config.watch) {
        config.plugins.push(
            new BrowserSyncPlugin(
                {
                    host: 'localhost',
                    port: 9000,
                    https: tls,
                    proxy: {
                        target: `http${tls ? 's' : ''}://localhost:${targetOptions.target === 'serve' ? '4200' : '8080'}`,
                        ws: true,
                        proxyOptions: {
                            changeOrigin: false, // pass the Host header to the backend unchanged  https://github.com/Browsersync/browser-sync/issues/430
                        },
                    },
                    socket: {
                        clients: {
                            heartbeatTimeout: 60000,
                        },
                    },
                },
                {
                    reload: targetOptions.target === 'build', // enabled for build --watch
                }
            )
        );
    }

    // PLUGINS
    if (config.mode === 'development') {
    config.plugins.push(
        new ESLintPlugin({
            extensions: ['js', 'ts'],
        }),
        new WebpackNotifierPlugin({
            title: 'Artemis',
            contentImage: path.join(__dirname, 'logo.png'),
        }));
    }

    if (config.mode === 'production') {
        config.plugins.push(
            new BundleAnalyzerPlugin({
                analyzerMode: 'static',
                openAnalyzer: false,
                // Webpack statistics in target folder
                reportFilename: '../stats.html',
            })
        );
    }

    const patterns = [
        { from: './src/main/resources/public/images/favicon.ico', to: 'public/images/favicon.ico' },
    ];

    if (patterns.length > 0) {
        config.plugins.push(new CopyWebpackPlugin({ patterns }));
    }

    config.resolve = {
        fallback: {
            'stream': require.resolve('stream-browserify'),
            'crypto': require.resolve('crypto-browserify'),
        }
    };

    config.plugins.push(
        new webpack.DefinePlugin({
            __TIMESTAMP__: JSON.stringify(environment.__TIMESTAMP__),
            // APP_VERSION is passed as an environment variable from the Gradle / Maven build tasks.
            __VERSION__: JSON.stringify(environment.__VERSION__),
            __DEBUG_INFO_ENABLED__: environment.__DEBUG_INFO_ENABLED__ || config.mode === 'development',
            // The root URL for API calls, ending with a '/' - for example: `"https://www.jhipster.tech:8081/myservice/"`.
            // If this URL is left empty (""), then it will be relative to the current context.
            // If you use an API server, in `prod` mode, you will need to enable CORS
            // (see the `jhipster.cors` common JHipster property in the `application-*.yml` configurations)
            __SERVER_API_URL__: JSON.stringify(environment.__SERVER_API_URL__),
        }),
        new MergeJsonWebpackPlugin({
            output: {
                groupBy: [
                    { pattern: './src/main/webapp/i18n/en/*.json', fileName: './i18n/en.json' },
                    { pattern: './src/main/webapp/i18n/de/*.json', fileName: './i18n/de.json' },
                    // jhipster-needle-i18n-language-webpack - JHipster will add/remove languages in this array
                ],
            },
        })
    );

    config = merge(
        config
        // jhipster-needle-add-webpack-config - JHipster will add custom config
    );

    return config;
};
